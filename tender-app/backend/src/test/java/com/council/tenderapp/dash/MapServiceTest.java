package com.council.tenderapp.dash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.dash.dtos.MapBoundsDto;
import com.council.tenderapp.dash.dtos.MapCourierDto;
import com.council.tenderapp.dash.dtos.MapDataDto;
import com.council.tenderapp.dash.dtos.MapRestaurantDto;
import com.council.tenderapp.dash.repositories.MapRepository;
import com.council.tenderapp.dash.services.MapService;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {
    @Mock private MapRepository mapRepository;

    @Test
    void emptyDatasetFallsBackToTheDefaultBounds() {
        when(mapRepository.restaurants()).thenReturn(List.of());
        when(mapRepository.onDutyCouriers()).thenReturn(List.of());

        MapDataDto data = new MapService(mapRepository).getMapData();

        assertThat(data.bounds()).isEqualTo(MapService.DEFAULT_BOUNDS);
        assertThat(data.restaurants()).isEmpty();
        assertThat(data.couriers()).isEmpty();
    }

    @Test
    void boundsWrapEveryRestaurantAndCourier() {
        when(mapRepository.restaurants()).thenReturn(List.of(new MapRestaurantDto(1L, "Maple & Main", "open", 45.51, -73.57)));
        when(mapRepository.onDutyCouriers()).thenReturn(List.of(new MapCourierDto(6L, "Leo", "idle", 45.53, -73.60)));

        assertThat(new MapService(mapRepository).getMapData().bounds())
                .isEqualTo(new MapBoundsDto(45.51, -73.60, 45.53, -73.57));
    }

    @Test
    void aSingleLocationIsItsOwnBounds() {
        when(mapRepository.restaurants()).thenReturn(List.of());
        when(mapRepository.onDutyCouriers()).thenReturn(List.of(new MapCourierDto(6L, "Leo", "carrying", 45.50, -73.58)));

        assertThat(new MapService(mapRepository).getMapData().bounds())
                .isEqualTo(new MapBoundsDto(45.50, -73.58, 45.50, -73.58));
    }
}
