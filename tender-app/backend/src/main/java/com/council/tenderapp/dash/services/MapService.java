package com.council.tenderapp.dash.services;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dash.dtos.MapBoundsDto;
import com.council.tenderapp.dash.dtos.MapCourierDto;
import com.council.tenderapp.dash.dtos.MapCustomerDto;
import com.council.tenderapp.dash.dtos.MapDataDto;
import com.council.tenderapp.dash.dtos.MapRestaurantDto;
import com.council.tenderapp.dash.repositories.MapRepository;
import com.council.tenderapp.reg.security.Session;

@Service
public class MapService {

    // Central Montreal, where the service runs; shown when nothing on the map has a location yet
    public static final MapBoundsDto DEFAULT_BOUNDS = new MapBoundsDto(45.49, -73.62, 45.54, -73.55);

    private final MapRepository mapRepository;

    public MapService(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    /**
     * Each role sees only what it needs: the operator sees everything, a customer the open restaurants and the
     * couriers bringing their orders, a courier their own pickup and drop-off, and a restaurant the couriers
     * handling its orders.
     */
    @Transactional(readOnly = true)
    public MapDataDto getMapData(Session viewer) {
        long id = viewer.accountId();
        return switch (viewer.role()) {
            case OPERATOR -> build(mapRepository.restaurants(), mapRepository.onDutyCouriers(), mapRepository.customers());
            case CUSTOMER -> build(mapRepository.openRestaurants(), mapRepository.couriersOfCustomerOrders(id), mapRepository.customer(id));
            case COURIER -> build(mapRepository.restaurantsOfCourierOrders(id), mapRepository.onDutyCourier(id),
                    mapRepository.customersOfCourierOrders(id));
            case STAFF -> viewer.restaurantId() == null
                    ? build(List.of(), List.of(), List.of())
                    : build(mapRepository.restaurant(viewer.restaurantId()), mapRepository.couriersOfRestaurantOrders(viewer.restaurantId()), List.of());
        };
    }

    private static MapDataDto build(List<MapRestaurantDto> restaurants, List<MapCourierDto> couriers, List<MapCustomerDto> customers) {
        return new MapDataDto(boundsOf(restaurants, couriers, customers), restaurants, couriers, customers);
    }

    private static MapBoundsDto boundsOf(List<MapRestaurantDto> restaurants, List<MapCourierDto> couriers, List<MapCustomerDto> customers) {
        List<double[]> points = Stream.of(
                restaurants.stream().map(r -> new double[] {r.latitude(), r.longitude()}),
                couriers.stream().map(c -> new double[] {c.latitude(), c.longitude()}),
                customers.stream().map(c -> new double[] {c.latitude(), c.longitude()}))
                .flatMap(s -> s)
                .toList();
        if (points.isEmpty()) return DEFAULT_BOUNDS;

        return new MapBoundsDto(
                points.stream().mapToDouble(p -> p[0]).min().getAsDouble(),
                points.stream().mapToDouble(p -> p[1]).min().getAsDouble(),
                points.stream().mapToDouble(p -> p[0]).max().getAsDouble(),
                points.stream().mapToDouble(p -> p[1]).max().getAsDouble());
    }
}
