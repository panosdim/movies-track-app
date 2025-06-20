package eu.deltasw.common.events.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieEvent {
    private EventType eventType;
    private String userId;
    private Integer movieId;
    private Integer rating;
}
