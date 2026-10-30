package com.council.tenderapp.operator.dto;

import java.util.Locale;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RestaurantStatus {
    OPEN, PAUSED, CLOSED;

    @JsonValue
    public String value() { return name().toLowerCase(Locale.ROOT); }

    @JsonCreator
    public static RestaurantStatus fromValue(String value) {
        for (RestaurantStatus status : values()) {
            if (status.value().equals(value)) return status;
        }
        throw new IllegalArgumentException("Status must be open paused or closed");
    }
}
