package eu.deltasw.tmdb_service.model.dto;

import java.util.ArrayList;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class WatchInfoRequest {
    @NotEmpty(message = "movies is required")
    private ArrayList<Integer> movies;
}
