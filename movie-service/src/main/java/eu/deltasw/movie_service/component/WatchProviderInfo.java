package eu.deltasw.movie_service.component;

import eu.deltasw.movie_service.repository.MovieRepository;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import info.movito.themoviedbapi.tools.TmdbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WatchProviderInfo {
    private static final Logger logger = LoggerFactory.getLogger(WatchProviderInfo.class);

    private final MovieRepository repository;
    private final TmdbApi tmdb;

    public WatchProviderInfo(MovieRepository repository, @Value("${tmdb.key}") String tmdbKey) {
        this.repository = repository;
        tmdb = new TmdbApi(tmdbKey);
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Scheduled(fixedRate = 10000) // every 10 seconds
    public void updateWatchProvidersInfo() {
        logger.info("Updating watch providers info...");

        repository.findAll().forEach(movie -> {
            try {
                WatchProviders watchProviders = tmdb.getMovies().getWatchProviders(movie.getMovieId())
                        .getResults().get("GR");
                if (watchProviders != null) {
                    movie.setWatchProviders(watchProviders);
                    repository.save(movie);
                }
            } catch (TmdbException e) {
                logger.error("Error getting watch providers", e);
            }
        });

    }
}
