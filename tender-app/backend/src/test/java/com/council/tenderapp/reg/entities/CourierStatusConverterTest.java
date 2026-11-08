package com.council.tenderapp.reg.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CourierStatusConverterTest {

    private final CourierStatusConverter converter = new CourierStatusConverter();

    @Test
    void storesEveryStateAsTheLowercaseValueTheSeedUses() {
        for (CourierStatus status : CourierStatus.values()) {
            String column = converter.convertToDatabaseColumn(status);
            assertThat(column).isEqualTo(status.name().toLowerCase());
            assertThat(converter.convertToEntityAttribute(column)).isEqualTo(status);
        }
    }
}
