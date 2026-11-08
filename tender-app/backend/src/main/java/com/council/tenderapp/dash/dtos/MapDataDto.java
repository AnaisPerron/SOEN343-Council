package com.council.tenderapp.dash.dtos;

import java.util.List;

/** Everything the map draws; bounds falls back to a default area when there is nothing to place. */
public record MapDataDto(
    MapBoundsDto bounds,
    List<MapRestaurantDto> restaurants,
    List<MapCourierDto> couriers
) {}
