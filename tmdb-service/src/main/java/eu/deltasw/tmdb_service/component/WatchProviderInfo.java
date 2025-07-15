package eu.deltasw.tmdb_service.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import eu.deltasw.common.events.model.EventType;
import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.common.model.WatchInfo;
import eu.deltasw.common.service.MovieEventProducer;
import eu.deltasw.tmdb_service.repository.MovieRepository;
import eu.deltasw.tmdb_service.service.WatchProvidersMapperService;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.tools.TmdbException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WatchProviderInfo {
    private final MovieRepository repository;
    private final TmdbApi tmdb;
    private final MovieEventProducer movieEventProducer;
    private final WatchProvidersMapperService watchProvidersMapperService;

    @Value("${watchproviders.update.cron}")
    private String updateCron;

    public WatchProviderInfo(MovieRepository repository, @Value("${tmdb.key}") String tmdbKey,
            MovieEventProducer movieEventProducer, WatchProvidersMapperService watchProvidersMapperService) {
        this.repository = repository;
        this.tmdb = new TmdbApi(tmdbKey);
        this.movieEventProducer = movieEventProducer;
        this.watchProvidersMapperService = watchProvidersMapperService;
    }

    @PostConstruct
    public void init() {
        log.info("Scheduling watch provider updates with cron: {}", updateCron);
    }

    @Scheduled(cron = "${watchproviders.update.cron}")
    public void updateWatchProvidersInfo() {
        log.info("Updating watch providers info...");

        repository.findAll().forEach(movie -> {
            try {
                // Check if watch providers have changed
                WatchInfo existingProviders = movie.getWatchProviders();
                WatchInfo newProviders = watchProvidersMapperService.convertTo(
                        tmdb.getMovies().getWatchProviders(movie.getMovieId())
                                .getResults().get("GR"));
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
