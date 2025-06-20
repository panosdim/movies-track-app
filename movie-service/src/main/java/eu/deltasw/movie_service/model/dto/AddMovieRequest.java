package eu.deltasw.movie_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMovieRequest {
    @NotNull(message = "Movie ID is required")
    private Integer movieId;
    @NotBlank(message = "title is required")
    private String title;
    @NotBlank(message = "poster is required")
    private String poster;
}
