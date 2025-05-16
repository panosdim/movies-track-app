package eu.deltasw.movie_service.repository;

import eu.deltasw.movie_service.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByUserId(String userId);

    List<Movie> findByUserIdAndWatchedIsFalseOrWatchedIsNull(String userId);
}

