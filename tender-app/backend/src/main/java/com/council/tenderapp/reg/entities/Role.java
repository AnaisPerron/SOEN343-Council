package com.council.tenderapp.reg.entities;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    CUSTOMER,
    COURIER,
    STAFF,
    OPERATOR;

    @JsonCreator
    public static Role fromJson(String value) {
        return value == null ? null : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
