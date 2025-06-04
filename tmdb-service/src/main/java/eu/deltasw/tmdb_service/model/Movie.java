package eu.deltasw.tmdb_service.model;

import org.hibernate.annotations.Type;

import info.movito.themoviedbapi.model.core.watchproviders.WatchProviders;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer movieId;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private WatchProviders watchProviders;
}
