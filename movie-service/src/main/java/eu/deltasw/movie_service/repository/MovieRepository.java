package eu.deltasw.movie_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import eu.deltasw.movie_service.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByUserIdAndWatchedIsFalseOrWatchedIsNull(String userId);

    List<Movie> findByUserIdAndWatchedIsTrue(String userId);
}
