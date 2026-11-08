package com.council.tenderapp.reg.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

// courier_profiles.status is stored lowercase (offline, idle, assigned, carrying)
@Converter
public class CourierStatusConverter implements AttributeConverter<CourierStatus, String> {

    @Override
    public String convertToDatabaseColumn(CourierStatus status) {
        return status == null ? null : status.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public CourierStatus convertToEntityAttribute(String value) {
        return value == null ? null : CourierStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
