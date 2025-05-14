package eu.deltasw.common.events.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {
    ADD,
    RATE,
    DELETE;

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
