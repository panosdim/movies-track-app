package eu.deltasw.movie_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import eu.deltasw.common.exception.autoconfigure.ExceptionHandlerAutoConfiguration;
import eu.deltasw.common.security.autoconfigure.JwtFilterAutoConfiguration;

@EnableScheduling
@SpringBootApplication
@Import({ ExceptionHandlerAutoConfiguration.class, JwtFilterAutoConfiguration.class })
@EnableFeignClients
public class MovieServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }

}
