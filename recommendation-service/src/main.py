import argparse
import base64
import glob
import logging.config
import os
import queue
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from contextlib import asynccontextmanager
from typing import Optional

import fluent.handler
import numpy as np
import schedule
import uvicorn as uvicorn
from database import get_movie_ids, get_movie_ratings
from fastapi import Depends, FastAPI, Header, HTTPException, status
from fastapi.encoders import jsonable_encoder
from fastapi.responses import JSONResponse
from jose import JWTError, jwt
from kafka_consumer import KafkaMovieEventConsumer
from models import MovieEvent
from preprocess import preprocess_movie_data
from py_eureka_client import eureka_client
from recommendation_model import build_model, train_model
from tensorflow.keras.models import Model, load_model  # type: ignore
from tmdb import fetch_movie_details, fetch_new_releases

# Get logger for this module
_logger = logging.getLogger(__name__)

# JWT Configuration
JWT_SECRET_FROM_ENV = os.getenv("JWT_SECRET")
if not JWT_SECRET_FROM_ENV:
    raise ValueError("JWT_SECRET environment variable must be set")
JWT_SECRET_KEY = base64.b64decode(JWT_SECRET_FROM_ENV)
ALGORITHM = "HS256"


# Models directory configuration
def get_models_dir():
    """Get models directory with multiple fallbacks"""
    # Try environment variable first
    env_dir = os.getenv("MODELS_DIR")
    if env_dir:
        return env_dir

    # Try container path if /app exists and is writable
    container_path = "/app/models"
    try:
        if os.path.exists("/app"):
            os.makedirs(container_path, exist_ok=True)
            return container_path
    except PermissionError:
        pass

    # Fall back to local development path
    local_path = os.path.join(os.getcwd(), "models")
    return local_path


MODELS_DIR = get_models_dir()

# Ensure models directory exists
os.makedirs(MODELS_DIR, exist_ok=True)

# Initialize globals
kafka_consumer: KafkaMovieEventConsumer | None = None
movie_ids = []
ratings = []
genre_list = []
actor_list = []
director_list = []
model: Model | None = None
num_movies = 0

# Kafka configuration from environment variables
KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
KAFKA_GROUP_ID = "movie-recommendation-service"
KAFKA_TOPIC = "movie-events"

# User data cache
USER_DATA_CACHE: dict[str, dict] = {}
USER_DATA_CACHE_TTL = 7200  # 2 hours
USER_DATA_TIMESTAMPS: dict[str, float] = {}

# Suggestions cache
SUGGESTIONS_CACHE: dict[str, list] = {}
SUGGESTIONS_CACHE_TTL = 86400  # 1 day (24 hours)
SUGGESTIONS_TIMESTAMPS: dict[str, float] = {}

# Training queue and worker setup
training_queue = queue.Queue()
training_executor = ThreadPoolExecutor(max_workers=1)  # Only one training at a time

# Scheduler executor
scheduler_executor = ThreadPoolExecutor(max_workers=1)


async def get_current_user_id(authorization: Optional[str] = Header(None)) -> str:
    """
    Dependency to get user ID from JWT token in Authorization header.
    """
    if authorization is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization header missing",
            headers={"WWW-Authenticate": "Bearer"},
        )

    parts = authorization.split()

    if parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication scheme",
            headers={"WWW-Authenticate": "Bearer"},
        )
    elif len(parts) == 1 or len(parts) > 2:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token format",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = parts[1]
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, JWT_SECRET_KEY, algorithms=[ALGORITHM])
        user_id: Optional[str] = payload.get("sub")
        if user_id is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
    return user_id


def get_model_path(user_id: str) -> str:
    """Get the full path for a user's model file"""
    return os.path.join(MODELS_DIR, f"{user_id}.keras")


def get_all_user_ids() -> list[str]:
    """Get all user IDs by checking existing keras model files"""
    try:
        keras_pattern = os.path.join(MODELS_DIR, "*.keras")
        keras_files = glob.glob(keras_pattern)
        user_ids = [os.path.splitext(filename)[0] for filename in keras_files]
        _logger.info(f"Found {len(user_ids)} users with trained models: {user_ids}")
        return user_ids
    except Exception as e:
        _logger.error(f"Error getting user IDs: {e}")
        return []


def get_suggestions_cache(user_id: str) -> list | None:
    """Get cached suggestions if valid"""
    current_time = time.time()

    if (
        user_id in SUGGESTIONS_CACHE
        and user_id in SUGGESTIONS_TIMESTAMPS
        and current_time - SUGGESTIONS_TIMESTAMPS[user_id] < SUGGESTIONS_CACHE_TTL
    ):
        return SUGGESTIONS_CACHE[user_id]

    return None


def set_suggestions_cache(user_id: str, suggestions: list):
    """Cache suggestions for a user"""
    SUGGESTIONS_CACHE[user_id] = suggestions
    SUGGESTIONS_TIMESTAMPS[user_id] = time.time()
    _logger.info(f"Cached {len(suggestions)} suggestions for user: {user_id}")


def get_user_data_cache(user_id: str) -> dict | None:
    """Get cached user data (genre_list, actor_list, director_list) if valid"""
    current_time = time.time()

    if (
        user_id in USER_DATA_CACHE
        and user_id in USER_DATA_TIMESTAMPS
        and current_time - USER_DATA_TIMESTAMPS[user_id] < USER_DATA_CACHE_TTL
    ):
        return USER_DATA_CACHE[user_id]

    return None


def set_user_data_cache(
    user_id: str,
    genre_list: list[str],
    actor_list: list[str],
    director_list: list[str],
    num_movies: int,
):
    """Cache user data"""
    USER_DATA_CACHE[user_id] = {
        "genre_list": genre_list,
        "actor_list": actor_list,
        "director_list": director_list,
        "num_movies": num_movies,
    }
    USER_DATA_TIMESTAMPS[user_id] = time.time()


def populate_lists(user_id: str) -> tuple[list[str], list[str], list[str], int]:
    """
    Populate lists for a user, using cache when available
    Returns: (genre_list, actor_list, director_list, num_movies)
    """
    # Check cache first
    cached_data = get_user_data_cache(user_id)
    if cached_data:
        return (
            cached_data["genre_list"],
            cached_data["actor_list"],
            cached_data["director_list"],
            cached_data["num_movies"],
        )

    # Compute user data
    movie_ids, ratings = get_movie_ratings(user_id)
    unique_genres = set()
    unique_actors = set()
    unique_directors = set()

    # Loop through each movie to collect unique genres, actors, and directors
    for movie_id in movie_ids:
        movie_details = fetch_movie_details(movie_id)
        if movie_details:
            unique_genres.update(movie_details["genres"])
            unique_actors.update(movie_details["actors"])
            unique_directors.update(movie_details["director"])

    # Convert sets to sorted lists for consistent indexing
    genre_list = sorted(list(unique_genres))
    actor_list = sorted(list(unique_actors))
    director_list = sorted(list(unique_directors))
    num_movies = len(movie_ids)

    # Cache the computed data
    set_user_data_cache(user_id, genre_list, actor_list, director_list, num_movies)

    return genre_list, actor_list, director_list, num_movies


def compute_user_suggestions(user_id: str) -> list:
    """Compute suggestions for a specific user"""
    try:
        _logger.info(f"Computing suggestions for user: {user_id}")

        # Load user's model
        try:
            model_path = get_model_path(user_id)
            user_model = load_model(model_path)
        except Exception as e:
            _logger.error(f"Failed to load model for user {user_id}: {e}")
            return []

        # Get user-specific data
        user_genre_list, user_actor_list, user_director_list, user_num_movies = (
            populate_lists(user_id)
        )

        # Fetch new movie ids
        releases_1 = fetch_new_releases(1) or []
        releases_2 = fetch_new_releases(2) or []
        new_releases = releases_1 + releases_2
        watchlist_movie_ids = get_movie_ids(user_id)

        # Filter out movies that are in the watchlist
        filtered_movies = [
            movie for movie in new_releases if movie["id"] not in watchlist_movie_ids
        ]

        if not filtered_movies:
            return []

        # Batch process movie data preparation
        def fetch_and_preprocess_movie(movie):
            try:
                movie_details = fetch_movie_details(movie["id"])
                if movie_details:
                    movie_data = preprocess_movie_data(
                        movie_details,
                        user_genre_list,
                        user_actor_list,
                        user_director_list,
                    )
                    return movie, movie_data
            except Exception as e:
                _logger.error(f"Error processing movie {movie['id']}: {e}")
            return None

        # Parallel movie data preparation
        processed_movies = []
        with ThreadPoolExecutor(max_workers=20) as executor:
            future_to_movie = {
                executor.submit(fetch_and_preprocess_movie, movie): movie
                for movie in filtered_movies
            }

            for future in as_completed(future_to_movie):
                result = future.result()
                if result:
                    processed_movies.append(result)

        if not processed_movies:
            return []

        # Prepare batch data for model prediction
        batch_size = len(processed_movies)

        # Initialize batch arrays
        user_ids = np.zeros(batch_size, dtype=np.int32)
        movie_indices = np.full(batch_size, user_num_movies, dtype=np.int32)
        genre_vectors = np.zeros((batch_size, len(user_genre_list)), dtype=np.float32)
        release_years = np.zeros((batch_size, 1), dtype=np.float32)
        durations = np.zeros((batch_size, 1), dtype=np.float32)
        popularities = np.zeros((batch_size, 1), dtype=np.float32)
        actor_vectors = np.zeros((batch_size, len(user_actor_list)), dtype=np.float32)
        director_vectors = np.zeros(
            (batch_size, len(user_director_list)), dtype=np.float32
        )
        average_ratings = np.zeros((batch_size, 1), dtype=np.float32)

        # Fill batch arrays
        movies_list = []
        for i, (movie, movie_data) in enumerate(processed_movies):
            movies_list.append(movie)
            user_ids[i] = 0  # Single user
            genre_vectors[i] = movie_data["genre_vector"]
            release_years[i, 0] = movie_data["release_year"]
            durations[i, 0] = movie_data["duration"]
            popularities[i, 0] = movie_data["popularity"]
            actor_vectors[i] = movie_data["actor_vector"]
            director_vectors[i] = movie_data["director_vector"]
            average_ratings[i, 0] = movie_data["average_rating"]

        # Single batch prediction for all movies
        try:
            batch_predictions = user_model.predict(
                [
                    user_ids,
                    movie_indices,
                    genre_vectors,
                    release_years,
                    durations,
                    popularities,
                    actor_vectors,
                    director_vectors,
                    average_ratings,
                ],
                batch_size=batch_size,
            )

            # Apply predictions to movies
            for i, movie in enumerate(movies_list):
                movie["predicted_rating"] = float(batch_predictions[i][0] * 5)

        except Exception as e:
            _logger.error(f"Error during batch prediction for user {user_id}: {e}")
            return []

        # Sort by predicted rating in descending order
        sorted_movies = sorted(
            movies_list, key=lambda x: x["predicted_rating"], reverse=True
        )

        _logger.info(f"Computed {len(sorted_movies)} suggestions for user: {user_id}")
        return sorted_movies

    except Exception as e:
        _logger.error(f"Error computing suggestions for user {user_id}: {e}")
        return []


def update_all_user_suggestions():
    """Update suggestions cache for all users"""
    _logger.info("Starting scheduled update of all user suggestions")
    start_time = time.time()

    user_ids = get_all_user_ids()
    total_users = len(user_ids)

    if total_users == 0:
        _logger.info("No users found with trained models")
        return

    successful_updates = 0

    for i, user_id in enumerate(user_ids, 1):
        try:
            _logger.info(f"Updating suggestions for user {user_id} ({i}/{total_users})")
            suggestions = compute_user_suggestions(user_id)
            set_suggestions_cache(user_id, suggestions)
            successful_updates += 1
        except Exception as e:
            _logger.error(f"Failed to update suggestions for user {user_id}: {e}")

    end_time = time.time()
    duration = end_time - start_time

    _logger.info(
        f"Scheduled update completed: {successful_updates}/{total_users} users updated in {duration:.2f} seconds"
    )


def populate_lists_legacy(user_id: str):
    """Legacy function for backward compatibility - updates global variables"""
    global movie_ids, ratings, genre_list, actor_list, director_list, num_movies

    genre_list, actor_list, director_list, num_movies = populate_lists(user_id)
    movie_ids, ratings = get_movie_ratings(user_id)


def training_worker():
    """Worker function that processes training requests sequentially"""
    while True:
        try:
            event_data = training_queue.get(timeout=60)  # Wait up to 60 seconds
            if event_data is None:  # Shutdown signal
                break

            _execute_training(event_data)
            training_queue.task_done()

        except queue.Empty:
            continue  # Timeout, continue waiting
        except Exception as e:
            _logger.error(f"Training worker error: {e}")
            training_queue.task_done()


def _execute_training(event_data: dict):
    """Execute the actual training process"""
    global model
    user_id = None

    try:
        event = MovieEvent(**event_data)
        user_id = event.userId

        _logger.info(f"Starting training for user: {user_id}")

        # Clear user cache when training (data might have changed)
        if user_id in USER_DATA_CACHE:
            del USER_DATA_CACHE[user_id]
        if user_id in USER_DATA_TIMESTAMPS:
            del USER_DATA_TIMESTAMPS[user_id]

        # Clear suggestions cache when training
        if user_id in SUGGESTIONS_CACHE:
            del SUGGESTIONS_CACHE[user_id]
        if user_id in SUGGESTIONS_TIMESTAMPS:
            del SUGGESTIONS_TIMESTAMPS[user_id]

        populate_lists_legacy(user_id)

        # Instantiate the model
        new_model = build_model(
            num_movies=num_movies + 1,
            num_genres=len(genre_list),
            num_actors=len(actor_list),
            num_directors=len(director_list),
        )

        # Train the model
        new_model = train_model(
            new_model, movie_ids, ratings, genre_list, actor_list, director_list
        )
        # Save the model to the configured models directory
        model_path = get_model_path(user_id)
        new_model.save(model_path)

        _logger.info(f"Training completed successfully for user: {user_id}")

        # Compute suggestions immediately after training
        try:
            _logger.info(f"Computing suggestions after training for user: {user_id}")
            suggestions = compute_user_suggestions(user_id)
            set_suggestions_cache(user_id, suggestions)
        except Exception as e:
            _logger.error(
                f"Error computing suggestions after training for user {user_id}: {e}"
            )

    except Exception as e:
        _logger.error(f"Error during training for user {user_id}: {e}")


def process_movie_event(event_data: dict):
    """Process a movie event from Kafka by queuing it for training"""
    try:
        event = MovieEvent(**event_data)
        user_id = event.userId

        _logger.info(f"Queuing training request for user: {user_id}")

        # Invalidate caches when a new event occurs
        if user_id in USER_DATA_CACHE:
            del USER_DATA_CACHE[user_id]
        if user_id in USER_DATA_TIMESTAMPS:
            del USER_DATA_TIMESTAMPS[user_id]
        if user_id in SUGGESTIONS_CACHE:
            del SUGGESTIONS_CACHE[user_id]
        if user_id in SUGGESTIONS_TIMESTAMPS:
            del SUGGESTIONS_TIMESTAMPS[user_id]

        training_queue.put(event_data)

    except Exception as e:
        _logger.error(f"Error processing event: {e}")


def run_scheduler():
    """Run the scheduler in a separate thread"""
    while True:
        schedule.run_pending()
        time.sleep(60)  # Check every minute


# Schedule the job to run every night at 2 AM
schedule.every().day.at("02:00").do(update_all_user_suggestions)

# Start the training worker thread
training_executor.submit(training_worker)

# Start the scheduler thread
scheduler_executor.submit(run_scheduler)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # Initialize components
    global kafka_consumer

    # Configure logging based on environment
    profile = os.getenv("APP_PROFILE", "docker")

    # Configure fluent logging
    if profile == "local":
        fluent_host = "localhost"
    else:
        fluent_host = "fluent-bit"

    # Setup console handler for all environments
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_formatter = logging.Formatter(
        "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    console_handler.setFormatter(console_formatter)

    # Setup handlers list with console handler
    handlers: list[logging.Handler] = [console_handler]

    # Try to set up fluent handler with error handling
    try:
        fluent_handler = fluent.handler.FluentHandler(
            "movies-track.recommendation-service",
            host=fluent_host,
            port=24224,
            timeout=3.0,  # Add timeout
            retry_limit=3,  # Add retry limit
            wait_on_retry=True,  # Wait between retries
        )
        fluent_handler.setLevel(logging.INFO)

        # Add custom formatter for fluent logs
        fluent_formatter = fluent.handler.FluentRecordFormatter(
            {
                "level": "%(level)s",
                "message": "%(message)s",
                "service": "%(service)s",
                "component": "%(component)s",
                "environment": "%(environment)s",
            }
        )
        fluent_handler.setFormatter(fluent_formatter)

        # Add additional fields to fluent logs
        def add_fluent_fields(record):
            record.service = "recommendation"
            record.component = "recommendation-service"
            record.environment = "local" if profile == "local" else "docker"
            record.level = record.levelname
            return True

        fluent_handler.addFilter(add_fluent_fields)

        # Add fluent handler to handlers list
        handlers.append(fluent_handler)
        print(f"Fluent logging configured with host: {fluent_host}")
    except Exception as e:
        print(f"Warning: Could not configure fluent logging: {e}")

    # Configure root logger with all available handlers
    logging.basicConfig(
        level=logging.INFO,
        handlers=handlers,
    )

    # Get logger for this module
    _logger = logging.getLogger(__name__)
    _logger.info(f"Logging initialized in {profile} mode with {len(handlers)} handlers")

    eureka_url = (
        "http://localhost:8761/eureka/"
        if profile == "local"
        else "http://eureka:8761/eureka/"
    )

    # Initialize Eureka client asynchronously
    try:
        if profile == "local":
            # Local development configuration
            await eureka_client.init_async(
                eureka_server=eureka_url,
                app_name="recommendation-service",
                instance_port=8005,
                instance_host="localhost",
                renewal_interval_in_secs=5,
            )
        else:
            await eureka_client.init_async(
                eureka_server=eureka_url,
                app_name="recommendation-service",
                instance_port=8005,
                renewal_interval_in_secs=5,
            )
        _logger.info("Successfully registered with Eureka")
    except Exception as e:
        _logger.error(f"Failed to register with Eureka: {e}")

    KAFKA_BOOTSTRAP_SERVERS = "localhost:9092" if profile == "local" else "broker:29092"

    # Start Kafka consumer
    kafka_consumer = KafkaMovieEventConsumer(
        KAFKA_BOOTSTRAP_SERVERS, KAFKA_GROUP_ID, KAFKA_TOPIC, process_movie_event
    )
    kafka_consumer.start()

    # Run initial suggestions update for all users on startup
    _logger.info("Running initial suggestions update on startup")
    scheduler_executor.submit(update_all_user_suggestions)

    yield

    # Cleanup
    if kafka_consumer:
        kafka_consumer.stop()


app = FastAPI(title="Movie Recommendations API", lifespan=lifespan)


@app.get("/version")
def version():
    return JSONResponse(content=jsonable_encoder({"version": "2.0"}))


@app.get("/health")
def health():
    return {"status": "UP"}


@app.get("/info")
def info():
    return {"service": "recommendation-service", "version": "2.0"}


@app.get("/suggestion")
async def suggestion(
    numOfMovies: int = 20, user_id: str = Depends(get_current_user_id)
):
    # Check cache first
    cached_suggestions = get_suggestions_cache(user_id)
    if cached_suggestions is not None:
        _logger.info(f"Returning cached suggestions for user: {user_id}")
        return JSONResponse(
            status_code=200, content=jsonable_encoder(cached_suggestions)
        )

    # If not in cache, compute suggestions
    _logger.info(f"Computing suggestions for user: {user_id} (not in cache)")
    suggestions_list = compute_user_suggestions(user_id)

    if not suggestions_list:
        return JSONResponse(
            status_code=404,
            content=jsonable_encoder(
                {
                    "status": "error",
                    "message": f"No suggestions available for user {user_id}. Please ensure the model is trained.",
                }
            ),
        )

    # Cache the computed suggestions
    set_suggestions_cache(user_id, suggestions_list)

    return JSONResponse(status_code=200, content=jsonable_encoder(suggestions_list))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Movies Suggestions")
    parser.add_argument(
        "-p",
        "--port",
        help="Port number to run server",
        type=int,
        default=8005,
    )
    args = parser.parse_args()

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=args.port,
        reload=False,
        log_level="info",
        log_config=None,
    )
