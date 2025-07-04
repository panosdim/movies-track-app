package eu.deltasw.movie_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.deltasw.movie_service.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Query("SELECT m FROM Movie m WHERE m.userId = :userId AND (m.watched = false OR m.watched IS NULL)")
    List<Movie> findByUserIdAndWatchedIsFalseOrWatchedIsNull(@Param("userId") String userId);

    @Query("SELECT m FROM Movie m WHERE m.movieId = :movieId AND (m.watched = false OR m.watched IS NULL)")
    List<Movie> findUnwatchedByMovieId(@Param("movieId") Integer movieId);

    @Query("SELECT m FROM Movie m WHERE m.userId = :userId AND m.watched = true")
    List<Movie> findByUserIdAndWatchedIsTrue(@Param("userId") String userId);
}
