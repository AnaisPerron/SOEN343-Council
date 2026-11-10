package com.council.tenderapp.prc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.prc.repositories.BillRepository;

class PricingServiceTest {
    @Test
    void computesConfiguredFeesFromSubtotalAndDistance() {
        RestaurantRepository restaurants = mock(RestaurantRepository.class);
        Restaurant restaurant = new Restaurant("Bistro", "open", 0.0, 0.0);
        when(restaurants.findById(1L)).thenReturn(Optional.of(restaurant));
        PricingService pricing = new PricingService(restaurants, mock(BillRepository.class),
                new BigDecimal("0.80"), new BigDecimal("1.50"), new BigDecimal("15.00"), new BigDecimal("2.00"));

        PricingService.Quote small = pricing.quote(new BigDecimal("12.00"), 1L, 0.0, 0.0179864);
        assertThat(small.distanceKm()).isEqualByComparingTo("2.00");
        assertThat(small.deliveryFee()).isEqualByComparingTo("1.60");
        assertThat(small.smallOrderFee()).isEqualByComparingTo("2.00");
        assertThat(small.total()).isEqualByComparingTo("17.10");

        PricingService.Quote threshold = pricing.quote(new BigDecimal("15.00"), 1L, 0.0, 0.0179864);
        assertThat(threshold.smallOrderFee()).isEqualByComparingTo("0.00");
        assertThat(threshold.total()).isEqualByComparingTo("18.10");
    }
}
