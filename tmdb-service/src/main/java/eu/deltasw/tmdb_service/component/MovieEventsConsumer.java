package eu.deltasw.tmdb_service.component;

import eu.deltasw.common.events.model.EventType;
import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.tmdb_service.model.Movie;
import eu.deltasw.tmdb_service.repository.MovieRepository;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import info.movito.themoviedbapi.tools.TmdbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MovieEventsConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovieEventsConsumer.class);

    private final MovieRepository repository;
    private final TmdbApi tmdb;

    public MovieEventsConsumer(MovieRepository repository, @Value("${tmdb.key}") String tmdbKey) {
        this.repository = repository;
        tmdb = new TmdbApi(tmdbKey);
    }

    @KafkaListener(
            topics = "movie-events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMovieEvent(MovieEvent event) {
        LOGGER.info("Received movie event: {}", event.toString());

        switch (event.getEventType()) {
            case EventType.ADD:
                LOGGER.info("Processing MOVIE_ADDED for movieId: {}", event.getMovieId());
                Movie movie = new Movie();
                movie.setMovieId(event.getMovieId());
                try {
                    WatchProviders watchProviders = tmdb.getMovies().getWatchProviders(movie.getMovieId())
                            .getResults().get("GR");
                    movie.setWatchProviders(watchProviders);
                    repository.save(movie);
                } catch (TmdbException e) {
                    LOGGER.error("Error getting watch providers", e);
                }
                break;
            case EventType.RATE, EventType.DELETE:
                break;
            default:
                LOGGER.warn("Received unknown event type: {}", event.getEventType());
        }
    }
}