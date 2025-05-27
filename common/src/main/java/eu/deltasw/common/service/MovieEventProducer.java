package eu.deltasw.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import eu.deltasw.common.events.model.MovieEvent;

public class MovieEventProducer {
    private final KafkaTemplate<String, MovieEvent> kafkaTemplate;
    private final String topic;

    public MovieEventProducer(KafkaTemplate<String, MovieEvent> kafkaTemplate,
            @Value("${movie.events.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMovieEvent(MovieEvent event) {
        kafkaTemplate.send(topic, event);
    }
}
