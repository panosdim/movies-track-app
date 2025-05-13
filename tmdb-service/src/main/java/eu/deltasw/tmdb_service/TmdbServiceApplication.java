package eu.deltasw.tmdb_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TmdbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmdbServiceApplication.class, args);
    }

}