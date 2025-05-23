package eu.deltasw.tmdb_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WatchInfoResponse {
    private Integer movieId;
    private WatchProvidersDto watchProviders;
}