package eu.deltasw.tmdb_service.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.deltasw.common.model.dto.WatchInfoRequest;
import eu.deltasw.common.model.dto.WatchInfoResponse;
import eu.deltasw.tmdb_service.model.dto.ErrorResponse;
import eu.deltasw.tmdb_service.model.dto.SearchMovieRequest;
import eu.deltasw.tmdb_service.repository.MovieRepository;
import eu.deltasw.tmdb_service.service.WatchProvidersMapperService;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import info.movito.themoviedbapi.tools.sortby.DiscoverMovieSortBy;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/")
@Slf4j
public class TMDbController {

    private final TmdbApi tmdb;
    private final MovieRepository repository;
    private final WatchProvidersMapperService watchProvidersMapperService;

    public TMDbController(TmdbApi tmdb, MovieRepository repository,
            WatchProvidersMapperService watchProvidersMapperService) {
        this.tmdb = tmdb;
        this.repository = repository;
        this.watchProvidersMapperService = watchProvidersMapperService;
    }

    @GetMapping("/popular")
    public ResponseEntity<?> getPopularMovies() {
        DiscoverMovieParamBuilder discoverMovieParamBuilder = new DiscoverMovieParamBuilder();
        discoverMovieParamBuilder.page(1);
        discoverMovieParamBuilder.sortBy(DiscoverMovieSortBy.POPULARITY_DESC);
        discoverMovieParamBuilder.region("GR");

        try {
            return ResponseEntity.ok(tmdb.getDiscover().getMovie(discoverMovieParamBuilder));
        } catch (TmdbException e) {
            log.warn("Cannot communicate with TMDb API {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot communicate with TMDb API"));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@Valid @RequestBody SearchMovieRequest term) {
        try {
            return ResponseEntity.ok(tmdb.getSearch().searchMovie(term.getTerm(), false, null, null, null, null, null));
        } catch (TmdbException e) {
            log.warn("Cannot communicate with TMDb API {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot communicate with TMDb API"));
        }
    }

    @PostMapping("/autocomplete")
    public ResponseEntity<?> autocomplete(@Valid @RequestBody SearchMovieRequest term) {
        try {
            var results = tmdb.getSearch().searchMovie(term.getTerm(), false, null, null, null, null, null);
            var response = results.getResults().stream()
                    .map(movie -> Arrays.asList(
                            movie.getTitle(),
                            movie.getReleaseDate(),
                            movie.getPosterPath() != null
                                    ? "https://image.tmdb.org/t/p/w92" + movie.getPosterPath()
                                    : null))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (TmdbException e) {
            log.warn("Cannot communicate with TMDb API {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot communicate with TMDb API"));
        }
    }

    @PostMapping("/watch-info")
    public ResponseEntity<?> watchInfo(@Valid @RequestBody WatchInfoRequest movies) {
        try {
            var movieIds = movies.getMovies();

            if (movieIds.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("movies is required"));
            }

            var results = repository.findByMovieIdIn(movieIds);
            List<WatchInfoResponse> watchInfoResponse = results.stream()
                    .map(movie -> {
                        Double userScore = null;
                        try {
                            // Get user score from TMDb API
                            var movieInfo = tmdb.getMovies().getDetails(movie.getMovieId(), "en", null);
                            // Return a map with movie ID and vote average
                            userScore = movieInfo.getVoteAverage();
                        } catch (TmdbException e) {
                            log.warn("Error fetching score for movie {}: {}", movie.getMovieId(), e.getMessage());
                        }
                        return new WatchInfoResponse(movie.getMovieId(), userScore, movie.getWatchProviders());
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(watchInfoResponse);
        } catch (Exception e) {
            log.warn("General error: {}", e.toString());
            return ResponseEntity.badRequest().body(new ErrorResponse("Database error"));
        }
    }
}
