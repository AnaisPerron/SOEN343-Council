package com.council.tenderapp.dash.dtos;

/** Smallest box holding every location on the map, so the client can fit its view to the data. */
public record MapBoundsDto(
    double minLatitude,
    double minLongitude,
    double maxLatitude,
    double maxLongitude
) {}
