package eu.deltasw.common.security.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import eu.deltasw.common.events.model.MovieEvent;
import eu.deltasw.common.service.MovieEventProducer;

@Configuration
@ConditionalOnClass(KafkaAutoConfiguration.class)
public class MovieEventProducerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MovieEventProducer movieEventProducer(KafkaTemplate<String, MovieEvent> kafkaTemplate,
            @Value("${movie.events.topic}") String topic) {
        return new MovieEventProducer(kafkaTemplate, topic);
    }
}
