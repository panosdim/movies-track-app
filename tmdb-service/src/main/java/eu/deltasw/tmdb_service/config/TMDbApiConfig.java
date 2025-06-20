package eu.deltasw.tmdb_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import info.movito.themoviedbapi.TmdbApi;

@Configuration
public class TMDbApiConfig {
    @Bean
    TmdbApi tmdbApi(@Value("${tmdb.key}") String tmdbKey) {
        return new TmdbApi(tmdbKey);
    }
}
