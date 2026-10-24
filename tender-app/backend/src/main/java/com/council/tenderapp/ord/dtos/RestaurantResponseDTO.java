package com.council.tenderapp.ord.dtos;

public record RestaurantResponseDTO (

    Long id,
    String name,
    String status,
    Double latitude,
    Double longitude,
    String photoUrl
) {}
