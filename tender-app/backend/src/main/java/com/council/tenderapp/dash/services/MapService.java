package com.council.tenderapp.dash.services;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dash.dtos.MapBoundsDto;
import com.council.tenderapp.dash.dtos.MapCourierDto;
import com.council.tenderapp.dash.dtos.MapDataDto;
import com.council.tenderapp.dash.dtos.MapRestaurantDto;
import com.council.tenderapp.dash.repositories.MapRepository;

@Service
public class MapService {

    // Central Montreal, where the service runs; shown when nothing on the map has a location yet
    public static final MapBoundsDto DEFAULT_BOUNDS = new MapBoundsDto(45.49, -73.62, 45.54, -73.55);

    private final MapRepository mapRepository;

    public MapService(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    @Transactional(readOnly = true)
    public MapDataDto getMapData() {
        List<MapRestaurantDto> restaurants = mapRepository.restaurants();
        List<MapCourierDto> couriers = mapRepository.onDutyCouriers();
        return new MapDataDto(boundsOf(restaurants, couriers), restaurants, couriers);
    }

    private static MapBoundsDto boundsOf(List<MapRestaurantDto> restaurants, List<MapCourierDto> couriers) {
        List<double[]> points = Stream.concat(
                restaurants.stream().map(r -> new double[] {r.latitude(), r.longitude()}),
                couriers.stream().map(c -> new double[] {c.latitude(), c.longitude()}))
                .toList();
        if (points.isEmpty()) return DEFAULT_BOUNDS;

        return new MapBoundsDto(
                points.stream().mapToDouble(p -> p[0]).min().getAsDouble(),
                points.stream().mapToDouble(p -> p[1]).min().getAsDouble(),
                points.stream().mapToDouble(p -> p[0]).max().getAsDouble(),
                points.stream().mapToDouble(p -> p[1]).max().getAsDouble());
    }
}
