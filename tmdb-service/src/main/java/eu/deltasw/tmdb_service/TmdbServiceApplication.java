package eu.deltasw.tmdb_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import eu.deltasw.common.exception.autoconfigure.ExceptionHandlerAutoConfiguration;
import eu.deltasw.common.security.autoconfigure.MovieEventProducerAutoConfiguration;

@EnableScheduling
@SpringBootApplication
@Import({ ExceptionHandlerAutoConfiguration.class, MovieEventProducerAutoConfiguration.class })
public class TmdbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmdbServiceApplication.class, args);
    }

}