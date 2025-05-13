package eu.deltasw.gateway.config;

import eu.deltasw.gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDiscoveryClient
public class RouteConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public RouteConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Open endpoints (auth)
                .route("auth_service", r -> r.path("/login", "/register")
                        .uri("lb://auth-service"))

                // Open endpoints (tmdb)
                .route("tmdb_service", r -> r.path("/popular")
                        .uri("lb://tmdb-service"))

                // Secured endpoints (movies)
                .route("movie_service", r -> r.path("/movies/**")
                        .filters(f -> f.filter(jwtFilter)) // JWT filter here
                        .uri("lb://movie-service"))
                .build();
    }
}
