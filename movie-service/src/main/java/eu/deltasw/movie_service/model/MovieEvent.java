package eu.deltasw.movie_service.model;

import lombok.Data;

@Data
public class MovieEvent {
    private EventType eventType;
    private String userId;
    private Integer movieId;
    private Integer rating;

    public MovieEvent(EventType eventType, Movie movie) {
        this.eventType = eventType;
        this.userId = movie.getUserId();
        this.movieId = movie.getId().intValue();
        this.rating = movie.getRating();
    }
}
