package eu.deltasw.tmdb_service.component;

import eu.deltasw.tmdb_service.repository.MovieRepository;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import info.movito.themoviedbapi.tools.TmdbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WatchProviderInfo {
    private final MovieRepository repository;
    private final TmdbApi tmdb;

    public WatchProviderInfo(MovieRepository repository, @Value("${tmdb.key}") String tmdbKey) {
        this.repository = repository;
        tmdb = new TmdbApi(tmdbKey);
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void updateWatchProvidersInfo() {
        log.info("Updating watch providers info...");

        repository.findAll().forEach(movie -> {
            try {
                WatchProviders watchProviders = tmdb.getMovies().getWatchProviders(movie.getMovieId())
                        .getResults().get("GR");
                if (watchProviders != null) {
                    movie.setWatchProviders(watchProviders);
                    repository.save(movie);
                }
            } catch (TmdbException e) {
                log.error("Error getting watch providers", e);
            }
        });
    }
}
