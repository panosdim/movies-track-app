package eu.deltasw.tmdb_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import eu.deltasw.tmdb_service.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByMovieId(Integer movieId);

    List<Movie> findByMovieIdIn(List<Integer> movieIds);
}
