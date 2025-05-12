package eu.deltasw.movie_service.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private Integer movieId;
    private String title;
    private String poster;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private WatchProviders watchProviders;
    private Boolean watched;
    private Integer rating;
}

