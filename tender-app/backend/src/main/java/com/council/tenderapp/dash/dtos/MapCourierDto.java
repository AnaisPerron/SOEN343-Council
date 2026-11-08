package com.council.tenderapp.dash.dtos;

public record MapCourierDto(
    Long courierId,
    String name,
    String status,
    double latitude,
    double longitude
) {}
