package eu.deltasw.movie_service.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {
    ADD,
    UPDATE,
    DELETE;

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
