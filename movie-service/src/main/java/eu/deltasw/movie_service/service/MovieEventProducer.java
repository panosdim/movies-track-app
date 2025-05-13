package eu.deltasw.movie_service.service;

import eu.deltasw.common.events.model.EventType;
import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.movie_service.model.Movie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MovieEventProducer {

    private final KafkaTemplate<String, MovieEvent> kafkaTemplate;
    private final String topic;

    public MovieEventProducer(KafkaTemplate<String, MovieEvent> kafkaTemplate,
                              @Value("${movie.events.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMovieEvent(Movie movie, EventType eventType) {
        MovieEvent event = new MovieEvent(eventType, movie.getUserId(), movie.getMovieId(), movie.getRating());
        kafkaTemplate.send(topic, event);
    }
}
