package com.council.tenderapp.dash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.dash.dtos.MapBoundsDto;
import com.council.tenderapp.dash.dtos.MapCourierDto;
import com.council.tenderapp.dash.dtos.MapCustomerDto;
import com.council.tenderapp.dash.dtos.MapDataDto;
import com.council.tenderapp.dash.dtos.MapRestaurantDto;
import com.council.tenderapp.dash.repositories.MapRepository;
import com.council.tenderapp.dash.services.MapService;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.security.Session;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {
    @Mock private MapRepository mapRepository;

    private static Session viewer(Role role, Long accountId, Long restaurantId) {
        return new Session("token", accountId, restaurantId, role, Instant.MAX);
    }

    private static final Session OPERATOR = viewer(Role.OPERATOR, 8L, null);

    @Test
    void emptyDatasetFallsBackToTheDefaultBounds() {
        when(mapRepository.restaurants()).thenReturn(List.of());
        when(mapRepository.onDutyCouriers()).thenReturn(List.of());

        MapDataDto data = new MapService(mapRepository).getMapData(OPERATOR);

        assertThat(data.bounds()).isEqualTo(MapService.DEFAULT_BOUNDS);
        assertThat(data.restaurants()).isEmpty();
        assertThat(data.couriers()).isEmpty();
        assertThat(data.customers()).isEmpty();
    }

    @Test
    void boundsWrapEveryRestaurantAndCourier() {
        when(mapRepository.restaurants()).thenReturn(List.of(new MapRestaurantDto(1L, "Maple & Main", "open", 45.51, -73.57, 0)));
        when(mapRepository.onDutyCouriers()).thenReturn(List.of(new MapCourierDto(6L, "Leo", "idle", 45.53, -73.60)));

        assertThat(new MapService(mapRepository).getMapData(OPERATOR).bounds())
                .isEqualTo(new MapBoundsDto(45.51, -73.60, 45.53, -73.57));
    }

    @Test
    void aSingleLocationIsItsOwnBounds() {
        when(mapRepository.restaurants()).thenReturn(List.of());
        when(mapRepository.onDutyCouriers()).thenReturn(List.of(new MapCourierDto(6L, "Leo", "carrying", 45.50, -73.58)));

        assertThat(new MapService(mapRepository).getMapData(OPERATOR).bounds())
                .isEqualTo(new MapBoundsDto(45.50, -73.58, 45.50, -73.58));
    }

    @Test
    void aCustomerSeesTheOpenRestaurantsTheirCouriersAndThemselves() {
        when(mapRepository.openRestaurants()).thenReturn(List.of(new MapRestaurantDto(1L, "Maple & Main", "open", 45.51, -73.57, 0)));
        when(mapRepository.couriersOfCustomerOrders(3L)).thenReturn(List.of(new MapCourierDto(6L, "Leo", "carrying", 45.52, -73.58)));
        when(mapRepository.customer(3L)).thenReturn(List.of(new MapCustomerDto(3L, "Cara", 45.53, -73.59)));

        MapDataDto data = new MapService(mapRepository).getMapData(viewer(Role.CUSTOMER, 3L, null));

        assertThat(data.restaurants()).extracting(MapRestaurantDto::id).containsExactly(1L);
        assertThat(data.couriers()).extracting(MapCourierDto::courierId).containsExactly(6L);
        assertThat(data.customers()).extracting(MapCustomerDto::customerId).containsExactly(3L);
        assertThat(data.bounds()).isEqualTo(new MapBoundsDto(45.51, -73.59, 45.53, -73.57));
        verify(mapRepository, never()).restaurants();
        verify(mapRepository, never()).onDutyCouriers();
    }

    @Test
    void aCourierSeesThemselvesAndTheirPickupAndDropOff() {
        when(mapRepository.restaurantsOfCourierOrders(6L)).thenReturn(List.of(new MapRestaurantDto(1L, "Maple & Main", "open", 45.51, -73.57, 0)));
        when(mapRepository.onDutyCourier(6L)).thenReturn(List.of(new MapCourierDto(6L, "Leo", "assigned", 45.52, -73.58)));
        when(mapRepository.customersOfCourierOrders(6L)).thenReturn(List.of(new MapCustomerDto(3L, "Cara", 45.53, -73.59)));

        MapDataDto data = new MapService(mapRepository).getMapData(viewer(Role.COURIER, 6L, null));

        assertThat(data.restaurants()).extracting(MapRestaurantDto::id).containsExactly(1L);
        assertThat(data.couriers()).extracting(MapCourierDto::courierId).containsExactly(6L);
        assertThat(data.customers()).extracting(MapCustomerDto::customerId).containsExactly(3L);
        verify(mapRepository, never()).restaurants();
        verify(mapRepository, never()).onDutyCouriers();
    }

    @Test
    void aRestaurantSeesItselfAndTheCouriersOnItsOrdersButNoCustomers() {
        when(mapRepository.restaurant(1L)).thenReturn(List.of(new MapRestaurantDto(1L, "Maple & Main", "open", 45.51, -73.57, 0)));
        when(mapRepository.couriersOfRestaurantOrders(1L)).thenReturn(List.of(new MapCourierDto(6L, "Leo", "assigned", 45.52, -73.58)));

        MapDataDto data = new MapService(mapRepository).getMapData(viewer(Role.STAFF, 2L, 1L));

        assertThat(data.restaurants()).extracting(MapRestaurantDto::id).containsExactly(1L);
        assertThat(data.couriers()).extracting(MapCourierDto::courierId).containsExactly(6L);
        assertThat(data.customers()).isEmpty();
        verify(mapRepository, never()).restaurants();
        verify(mapRepository, never()).onDutyCouriers();
    }

    @Test
    void staffWithoutARestaurantSeeAnEmptyMap() {
        MapDataDto data = new MapService(mapRepository).getMapData(viewer(Role.STAFF, 2L, null));

        assertThat(data.bounds()).isEqualTo(MapService.DEFAULT_BOUNDS);
        assertThat(data.restaurants()).isEmpty();
        assertThat(data.couriers()).isEmpty();
        assertThat(data.customers()).isEmpty();
    }
}
