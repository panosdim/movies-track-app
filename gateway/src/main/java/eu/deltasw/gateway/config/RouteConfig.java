package eu.deltasw.gateway.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDiscoveryClient
public class RouteConfig {
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Endpoints for auth-service
                .route("auth_service", r -> r.path("/login", "/register")
                        .uri("lb://auth-service"))

                // Endpoints for tmdb-service
                .route("tmdb_service", r -> r.path("/popular")
                        .uri("lb://tmdb-service"))

                // Endpoints for movie-service
                .route("movie_service", r -> r.path("/movies/**")
                        .uri("lb://movie-service"))
                .build();
    }
}
