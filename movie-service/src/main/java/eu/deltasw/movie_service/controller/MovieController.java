package eu.deltasw.movie_service.controller;


import eu.deltasw.common.events.model.EventType;
import eu.deltasw.movie_service.model.Movie;
import eu.deltasw.movie_service.model.dto.AddMovieRequest;
import eu.deltasw.movie_service.model.dto.ErrorResponse;
import eu.deltasw.movie_service.model.dto.RateRequest;
import eu.deltasw.movie_service.repository.MovieRepository;
import eu.deltasw.movie_service.security.JwtUtil;
import eu.deltasw.movie_service.service.MovieEventProducer;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies")
@Slf4j
public class MovieController {

    private final MovieRepository repository;
    private final JwtUtil jwtUtil;
    private final MovieEventProducer movieEventProducer;

    public MovieController(MovieRepository repository, JwtUtil jwtUtil, MovieEventProducer movieEventProducer) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.movieEventProducer = movieEventProducer;
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
    public ResponseEntity<?> addMovie(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody AddMovieRequest addMovie) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);

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
                    return ResponseEntity.ok(repository.save(movie));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<?> setRating(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable("id") Long id, @Valid @RequestBody RateRequest rateRequest) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);

        // Validation
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot extract email from JWT"));
        }

        return repository.findById(id)
                .filter(m -> m.getUserId().equals(userId))
                .map(movie -> {
                    movie.setRating(rateRequest.getRating());
                    movieEventProducer.sendMovieEvent(movie, EventType.RATE);
                    return ResponseEntity.ok(repository.save(movie));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@RequestHeader("Authorization") String authHeader, @PathVariable("id") Long id) {
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

