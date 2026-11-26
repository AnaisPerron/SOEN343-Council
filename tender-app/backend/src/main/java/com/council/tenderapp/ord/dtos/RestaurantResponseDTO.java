package com.council.tenderapp.ord.dtos;

public record RestaurantResponseDTO (

    Long id,
    String name,
    String status,
    Double latitude,
    Double longitude,
    String photoUrl,
    // Average restaurant stars (1-5, one decimal); null when the restaurant has no ratings.
    Double averageRating,
    long ratingCount
) {}
