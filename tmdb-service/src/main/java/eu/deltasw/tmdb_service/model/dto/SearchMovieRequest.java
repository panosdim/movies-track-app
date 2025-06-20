package eu.deltasw.tmdb_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchMovieRequest {
    @NotBlank(message = "term is required")
    private String term;
}
