package com.council.tenderapp.dlv.entities;

import java.util.Locale;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AssignmentStatusConverter implements AttributeConverter<AssignmentStatus, String> {
    @Override
    public String convertToDatabaseColumn(AssignmentStatus value) { return value == null ? null : value.value(); }
    @Override
    public AssignmentStatus convertToEntityAttribute(String value) {
        return value == null ? null : AssignmentStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
