package eu.deltasw.movie_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.common.model.dto.MovieNotifyRequest;
import eu.deltasw.movie_service.data.NotificationClient;
import eu.deltasw.movie_service.repository.MovieRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MovieEventListener {
    private final MovieRepository repository;
    private final NotificationClient notificationClient;

    public MovieEventListener(MovieRepository repository, NotificationClient notificationClient) {
        this.repository = repository;
        this.notificationClient = notificationClient;
    }

    @KafkaListener(topics = "${movie.events.topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleMovieEvent(MovieEvent event) {
        log.info("Received movie event: {}", event.toString());

        if (event.getMovieId() == null)
            return;

        switch (event.getEventType()) {
            case ADD, RATE, DELETE:
                break;
            case WATCH_INFO_UPDATED:
                var usersWithMovieInWatchList = repository
                        .findByMovieIdAndWatchedIsFalseOrWatchedIsNull(event.getMovieId());
                if (!usersWithMovieInWatchList.isEmpty()) {
                    var users = usersWithMovieInWatchList.stream()
                            .map(movie -> movie.getUserId())
                            .distinct()
                            .toList();
                    var movieTitle = usersWithMovieInWatchList.get(0).getTitle();
                    var moviePoster = usersWithMovieInWatchList.get(0).getPoster();

                    // Send notification
                    var notifyRequest = new MovieNotifyRequest();
                    notifyRequest.setUserIds(users);
                    notifyRequest.setMovieTitle(movieTitle);
                    notifyRequest.setMoviePoster(moviePoster);
                    try {
                        notificationClient.notify(notifyRequest);
                    } catch (FeignException e) {
                        log.error("Failed to send notification for movie: {}", event.getMovieId(), e);
                    }
                } else {
                    log.warn("No users found with movie {} in watchlist", event.getMovieId());
                }

                break;
            default:
                log.warn("Received unknown event type: {}", event.getEventType());
        }
    }
}
