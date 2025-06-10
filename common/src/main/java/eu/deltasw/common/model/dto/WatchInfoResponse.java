package eu.deltasw.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WatchInfoResponse {
    private Integer movieId;
    private Double userScore;
    private WatchProvidersDto watchProviders;
}