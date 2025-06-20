package eu.deltasw.common.model.dto;

import java.util.ArrayList;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WatchInfoRequest {
    @NotEmpty(message = "movies is required")
    private ArrayList<Integer> movies;
}
