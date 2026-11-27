package com.council.tenderapp.rvw.dtos;

import java.time.Instant;

public record OrderRatingViewDto(Long orderId, Integer stars, Integer restaurantStars, Integer courierStars,
        String comment, Instant createdAt) {}
