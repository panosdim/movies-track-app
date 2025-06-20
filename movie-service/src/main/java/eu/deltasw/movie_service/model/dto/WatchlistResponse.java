package eu.deltasw.movie_service.model.dto;

import eu.deltasw.common.model.dto.WatchProvidersDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WatchlistResponse {
    private Long id;
    private Integer movieId;
    private String title;
    private String poster;
    private Double userScore;
    private WatchProvidersDto watchInfo;
}
