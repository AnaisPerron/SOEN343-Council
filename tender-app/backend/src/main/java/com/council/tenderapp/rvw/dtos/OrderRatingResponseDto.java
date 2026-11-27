package com.council.tenderapp.rvw.dtos;

import java.time.Instant;

import com.council.tenderapp.rvw.entities.OrderRating;

public record OrderRatingResponseDto(Long id, Long orderId, Long restaurantId, Long courierId,
        Integer restaurantStars, Integer courierStars, String comment, Instant createdAt) {

    public static OrderRatingResponseDto from(OrderRating rating) {
        return new OrderRatingResponseDto(rating.getId(), rating.getOrderId(), rating.getRestaurantId(),
                rating.getCourierId(), rating.getRestaurantStars(), rating.getCourierStars(),
                rating.getComment(), rating.getCreatedAt());
    }
}
