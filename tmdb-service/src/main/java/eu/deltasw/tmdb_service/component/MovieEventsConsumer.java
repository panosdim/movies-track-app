package eu.deltasw.tmdb_service.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.tmdb_service.model.Movie;
import eu.deltasw.tmdb_service.repository.MovieRepository;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import info.movito.themoviedbapi.tools.TmdbException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MovieEventsConsumer {
    private final MovieRepository repository;
    private final TmdbApi tmdb;

    public MovieEventsConsumer(MovieRepository repository, @Value("${tmdb.key}") String tmdbKey) {
        this.repository = repository;
        tmdb = new TmdbApi(tmdbKey);
    }

    @KafkaListener(topics = "${movie.events.topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMovieEvent(MovieEvent event) {
        log.info("Received movie event: {}", event.toString());

        switch (event.getEventType()) {
            case ADD:
                log.info("Processing MOVIE_ADDED for movieId: {}", event.getMovieId());

                Movie movie = repository.findByMovieId(event.getMovieId())
                        .orElseGet(() -> {
                            Movie newMovie = new Movie();
                            newMovie.setMovieId(event.getMovieId());
                            return newMovie;
                        });

                try {
                    WatchProviders watchProviders = tmdb.getMovies().getWatchProviders(movie.getMovieId())
                            .getResults().get("GR");
                    movie.setWatchProviders(watchProviders);
                } catch (TmdbException e) {
                    log.error("Error getting watch providers", e);
                }

                repository.save(movie);
                break;
            case RATE, DELETE, WATCH_INFO_UPDATED:
                break;
            default:
                log.warn("Received unknown event type: {}", event.getEventType());
        }
    }
}