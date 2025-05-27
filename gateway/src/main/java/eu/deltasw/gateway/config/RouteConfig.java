package eu.deltasw.gateway.config;

import java.util.List;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableDiscoveryClient
public class RouteConfig {
        @SuppressWarnings("null")
        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
                return builder.routes()
                                // Endpoints for auth-service
                                .route("auth_service", r -> r.path("/login", "/register")
                                                .uri("lb://auth-service"))

                                // Endpoints for tmdb-service
                                .route("tmdb_service",
                                                r -> r.path("/popular", "/search", "/autocomplete")
                                                                .uri("lb://tmdb-service"))
                                // /watch-info only available in 'local' profile
                                .route("tmdb_service_watch_info_local",
                                                r -> r.path("/watch-info")
                                                                .and()
                                                                .predicate(exchange -> {
                                                                        String[] profiles = exchange
                                                                                        .getApplicationContext()
                                                                                        .getEnvironment()
                                                                                        .getActiveProfiles();
                                                                        for (String profile : profiles) {
                                                                                if ("local".equals(profile)) {
                                                                                        return true;
                                                                                }
                                                                        }
                                                                        return false;
                                                                })
                                                                .uri("lb://tmdb-service"))

                                // Endpoints for movie-service
                                .route("movie_service", r -> r.path("/movies/**")
                                                .uri("lb://movie-service"))
                                .build();
        }

        @Bean
        public CorsWebFilter corsWebFilter() {
                CorsConfiguration corsConfig = new CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(List.of("*"));
                corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(List.of("*"));
                corsConfig.setAllowCredentials(false);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", corsConfig);

                return new CorsWebFilter(source);
        }
}
