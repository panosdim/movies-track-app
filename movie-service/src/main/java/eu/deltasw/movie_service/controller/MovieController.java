package eu.deltasw.movie_service.controller;


import eu.deltasw.movie_service.model.EventType;
import eu.deltasw.movie_service.model.Movie;
import eu.deltasw.movie_service.model.dto.ErrorResponse;
import eu.deltasw.movie_service.model.dto.RateRequest;
import eu.deltasw.movie_service.repository.MovieRepository;
import eu.deltasw.movie_service.security.JwtUtil;
import eu.deltasw.movie_service.service.MovieEventProducer;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import info.movito.themoviedbapi.tools.TmdbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies")
public class MovieController {
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);


    private final MovieRepository repository;
    private final JwtUtil jwtUtil;
    private final MovieEventProducer movieEventProducer;
    private final TmdbApi tmdb;

    public MovieController(MovieRepository repository, JwtUtil jwtUtil, MovieEventProducer movieEventProducer, @Value("${tmdb.key}") String tmdbKey) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.movieEventProducer = movieEventProducer;
        tmdb = new TmdbApi(tmdbKey);
    }

    @GetMapping
    public ResponseEntity<?> getMovies(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);
        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return ResponseEntity.ok(repository.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> addMovie(@RequestHeader("Authorization") String authHeader, @RequestBody Movie movie) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }
        if (movie.getMovieId() == null || movie.getTitle() == null || movie.getPoster() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Missing required fields"));
        }

        movie.setUserId(userId);

        WatchProviders watchProviders = null;
        try {
            watchProviders = tmdb.getMovies().getWatchProviders(movie.getMovieId())
                    .getResults().get("GR");
        } catch (TmdbException e) {
            logger.error("Error getting watch providers", e);
        }
        if (watchProviders != null) {
            movie.setWatchProviders(watchProviders);
        }

        Movie savedMovie = repository.save(movie);
        movieEventProducer.sendMovieEvent(savedMovie, EventType.ADD);
        return ResponseEntity.ok(savedMovie);
    }

    @PostMapping("/{id}/watched")
    public ResponseEntity<?> setWatched(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable("id") Long id) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return repository.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .map(movie -> {
                    movie.setWatched(true);
                    movieEventProducer.sendMovieEvent(movie, EventType.UPDATE);
                    return ResponseEntity.ok(repository.save(movie));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<?> setRating(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable("id") Long id, @RequestBody RateRequest rateRequest) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        if (rateRequest.getRating() == null || rateRequest.getRating() < 1 || rateRequest.getRating() > 5) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Rating must be between 1 and 5"));
        }

        return repository.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .map(movie -> {
                    movie.setRating(rateRequest.getRating());
                    movieEventProducer.sendMovieEvent(movie, EventType.UPDATE);
                    return ResponseEntity.ok(repository.save(movie));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return repository.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .map(movie -> {
                    repository.delete(movie);
                    movieEventProducer.sendMovieEvent(movie, EventType.DELETE);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

