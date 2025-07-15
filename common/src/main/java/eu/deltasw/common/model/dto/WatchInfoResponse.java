package eu.deltasw.common.model.dto;

import eu.deltasw.common.model.WatchInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WatchInfoResponse {
    private Integer movieId;
    private Double userScore;
    private WatchInfo watchProviders;
}