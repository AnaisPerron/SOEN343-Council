package com.council.tenderapp.operator.dto;


public record RestaurantSummary(
        long id,
        String name,
        String status,
        Double latitude,
        Double longitude,
        String address,
        String phone) {}
