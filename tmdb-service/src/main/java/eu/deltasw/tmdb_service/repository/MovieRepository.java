package eu.deltasw.tmdb_service.repository;

import eu.deltasw.tmdb_service.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByMovieId(Integer movieId);
}

