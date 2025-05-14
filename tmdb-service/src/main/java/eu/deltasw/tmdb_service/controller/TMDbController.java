package eu.deltasw.tmdb_service.controller;

import eu.deltasw.tmdb_service.model.ErrorResponse;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import info.movito.themoviedbapi.tools.sortby.DiscoverMovieSortBy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Slf4j
public class TMDbController {

    private final TmdbApi tmdb;

    public TMDbController(@Value("${tmdb.key}") String tmdbKey) {
        tmdb = new TmdbApi(tmdbKey);
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
            log.warn("Cannot communicate with TMDb API");
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot communicate with TMDb API"));
        }
    }
}
