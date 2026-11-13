package com.council.tenderapp.dash.dtos;

public record MapRestaurantDto(
    Long id,
    String name,
    String status,
    double latitude,
    double longitude,
    long queueCount
) {}
