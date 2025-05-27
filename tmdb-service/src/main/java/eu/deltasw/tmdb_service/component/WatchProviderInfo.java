package eu.deltasw.tmdb_service.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import eu.deltasw.common.events.model.EventType;
import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.common.service.MovieEventProducer;
import eu.deltasw.tmdb_service.repository.MovieRepository;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import info.movito.themoviedbapi.tools.TmdbException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WatchProviderInfo {
    private final MovieRepository repository;
    private final TmdbApi tmdb;
    private final MovieEventProducer movieEventProducer;

    public WatchProviderInfo(MovieRepository repository, @Value("${tmdb.key}") String tmdbKey,
            MovieEventProducer movieEventProducer) {
        this.repository = repository;
        tmdb = new TmdbApi(tmdbKey);
        this.movieEventProducer = movieEventProducer;
    }

    @Value("${watchproviders.update.cron}")
    private String updateCron;

    @Scheduled(cron = "${watchproviders.update.cron}")
    public void updateWatchProvidersInfo() {
        log.info("Updating watch providers info...");

        repository.findAll().forEach(movie -> {
            try {
                // Check if watch providers have changed
                WatchProviders existingProviders = movie.getWatchProviders();
                WatchProviders newProviders = tmdb.getMovies().getWatchProviders(movie.getMovieId())
                        .getResults().get("GR");
                if (existingProviders != null && !existingProviders.equals(newProviders)) {
                    MovieEvent event = new MovieEvent(EventType.WATCH_INFO_UPDATED, null, movie.getMovieId(), null);
                    movieEventProducer.sendMovieEvent(event);
                    movie.setWatchProviders(newProviders);
                    repository.save(movie);
                }
                if (existingProviders == null && newProviders != null) {
                    MovieEvent event = new MovieEvent(EventType.WATCH_INFO_UPDATED, null, movie.getMovieId(), null);
                    movieEventProducer.sendMovieEvent(event);
                    movie.setWatchProviders(newProviders);
                    repository.save(movie);
                }
            } catch (TmdbException e) {
                log.error("Error getting watch providers", e);
            }
        });
    }
}
