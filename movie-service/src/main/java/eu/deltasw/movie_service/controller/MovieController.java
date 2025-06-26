package eu.deltasw.movie_service.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.deltasw.common.events.model.EventType;
import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.common.model.dto.WatchInfoRequest;
import eu.deltasw.common.model.dto.WatchInfoResponse;
import eu.deltasw.common.service.MovieEventProducer;
import eu.deltasw.common.util.RequestContext;
import eu.deltasw.movie_service.data.WatchInfoClient;
import eu.deltasw.movie_service.model.Movie;
import eu.deltasw.movie_service.model.dto.AddMovieRequest;
import eu.deltasw.movie_service.model.dto.ErrorResponse;
import eu.deltasw.movie_service.model.dto.RateRequest;
import eu.deltasw.movie_service.model.dto.WatchlistResponse;
import eu.deltasw.movie_service.repository.MovieRepository;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/movies")
@Slf4j
public class MovieController {

    private final MovieRepository repository;
    private final MovieEventProducer movieEventProducer;
    private final WatchInfoClient watchInfoClient;

    public MovieController(MovieRepository repository, MovieEventProducer movieEventProducer,
            WatchInfoClient watchInfoClient) {
        this.repository = repository;
        this.movieEventProducer = movieEventProducer;
        this.watchInfoClient = watchInfoClient;
    }

    @GetMapping("/watched")
    public ResponseEntity<?> getWatchedMovies() {
        String userId = RequestContext.getCurrentUserId();

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return ResponseEntity.ok(repository.findByUserIdAndWatchedIsTrue(userId));
    }

    @GetMapping("/watchlist")
    public ResponseEntity<?> getWatchlist() {
        String userId = RequestContext.getCurrentUserId();

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        var movies = repository.findByUserIdAndWatchedIsFalseOrWatchedIsNull(userId);

        // Fetch watch info for the movies
        var movieIds = new ArrayList<>(
                movies.stream()
                        .map(Movie::getMovieId)
                        .toList());

        log.info("Fetching watch info for movies: {}", movieIds);
        if (movieIds.isEmpty()) {
            return ResponseEntity.ok(movies); // Return an empty response
        }

        List<WatchInfoResponse> watchInfoResponse;
        try {
            // Fetch watch info from the watch info client
            watchInfoResponse = watchInfoClient.getWatchInfo(new WatchInfoRequest(movieIds));
        } catch (FeignException e) {
            log.error("Error fetching watch info", e);
            var response = movies.stream()
                    .map(movie -> new WatchlistResponse(
                            movie.getId(),
                            movie.getMovieId(),
                            movie.getTitle(),
                            movie.getPoster(),
                            0.0,
                            null)) // No watch info available
                    .toList();
            return ResponseEntity.ok(response); // Return movies without watch info data
        }

        List<WatchlistResponse> watchlistResponse = new ArrayList<>();
        log.info("Watch info response: {}", watchInfoResponse);

        // Combine the watch info and movies to MovieResponse
        watchInfoResponse.forEach(info -> movies.stream()
                .filter(movie -> movie.getMovieId().equals(info.getMovieId()))
                .findFirst()
                .ifPresent(movie -> watchlistResponse.add(new WatchlistResponse(
                        movie.getId(),
                        movie.getMovieId(),
                        movie.getTitle(),
                        movie.getPoster(),
                        info.getUserScore(),
                        info.getWatchProviders()))));

        return ResponseEntity.ok(watchlistResponse);
    }

    @PostMapping
    public ResponseEntity<?> addMovie(@Valid @RequestBody AddMovieRequest addMovie) {
        String userId = RequestContext.getCurrentUserId();

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        Movie movie = Movie.builder()
                .movieId(addMovie.getMovieId())
                .title(addMovie.getTitle())
                .poster(addMovie.getPoster())
                .userId(userId)
                .build();

        Movie savedMovie = repository.save(movie);
        MovieEvent event = new MovieEvent(EventType.ADD, movie.getUserId(), movie.getMovieId(), movie.getRating());
        movieEventProducer.sendMovieEvent(event);
        return ResponseEntity.ok(savedMovie);
    }

    @PostMapping("/watched/{id}")
    public ResponseEntity<?> setWatched(@PathVariable("id") Long id) {
        String userId = RequestContext.getCurrentUserId();

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return repository.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .map(movie -> {
                    movie.setWatched(true);
                    return ResponseEntity.ok(repository.save(movie));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rate/{id}")
    public ResponseEntity<?> setRating(@PathVariable("id") Long id, @Valid @RequestBody RateRequest rateRequest) {
        String userId = RequestContext.getCurrentUserId();

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return repository.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .map(movie -> {
                    movie.setRating(rateRequest.getRating());
                    MovieEvent event = new MovieEvent(EventType.RATE, movie.getUserId(), movie.getMovieId(),
                            movie.getRating());
                    movieEventProducer.sendMovieEvent(event);
                    return ResponseEntity.ok(repository.save(movie));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable("id") Long id) {
        String userId = RequestContext.getCurrentUserId();

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return repository.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .map(movie -> {
                    repository.delete(movie);
                    MovieEvent event = new MovieEvent(EventType.DELETE, movie.getUserId(), movie.getMovieId(),
                            movie.getRating());
                    movieEventProducer.sendMovieEvent(event);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
