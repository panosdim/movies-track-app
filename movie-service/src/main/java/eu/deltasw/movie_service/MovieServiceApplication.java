package eu.deltasw.movie_service;

import eu.deltasw.common.exception.autoconfigure.ExceptionHandlerAutoConfiguration;
import eu.deltasw.common.security.autoconfigure.JwtFilterAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@Import({ExceptionHandlerAutoConfiguration.class, JwtFilterAutoConfiguration.class})
public class MovieServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }

}
