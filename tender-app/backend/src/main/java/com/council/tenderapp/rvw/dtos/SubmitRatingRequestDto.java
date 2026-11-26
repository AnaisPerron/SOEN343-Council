package com.council.tenderapp.rvw.dtos;

import com.council.tenderapp.rvw.entities.OrderRating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/orders/{orderId}/rating. Stars are whole numbers from 1 to 5.
 * courierStars is required when a courier delivered the order and must be omitted otherwise.
 */
public record SubmitRatingRequestDto(
        @NotNull(message = "Restaurant rating is required")
        @Min(value = OrderRating.MIN_STARS, message = "Restaurant rating must be between 1 and 5")
        @Max(value = OrderRating.MAX_STARS, message = "Restaurant rating must be between 1 and 5")
        Integer restaurantStars,

        @Min(value = OrderRating.MIN_STARS, message = "Courier rating must be between 1 and 5")
        @Max(value = OrderRating.MAX_STARS, message = "Courier rating must be between 1 and 5")
        Integer courierStars,

        @Size(max = OrderRating.MAX_COMMENT_LENGTH, message = "Comment must be at most 500 characters")
        String comment) {
}
