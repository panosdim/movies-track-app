package eu.deltasw.common.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class MovieNotifyRequest {
    private List<String> userIds;
    private String movieTitle;
    private String moviePoster;
}
