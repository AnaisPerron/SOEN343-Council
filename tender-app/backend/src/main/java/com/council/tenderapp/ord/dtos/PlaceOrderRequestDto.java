package com.council.tenderapp.ord.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

public record PlaceOrderRequestDto(
    @NotNull(message = "Restaurant id is required")
    Long restaurantId,

    @NotEmpty(message = "Order must contain at least one item")
    List<@Valid @NotNull(message = "Order item is required") OrderItemRequestDto> items,
    @Size(max = 255) String deliveryAddress,
    @DecimalMin("-90") @DecimalMax("90") Double deliveryLatitude,
    @DecimalMin("-180") @DecimalMax("180") Double deliveryLongitude,
    Long tendersToUse
) {
    public PlaceOrderRequestDto(Long restaurantId, List<OrderItemRequestDto> items) {
        this(restaurantId, items, null, null, null, null);
    }

    public PlaceOrderRequestDto(Long restaurantId, List<OrderItemRequestDto> items, String address,
            Double latitude, Double longitude) {
        this(restaurantId, items, address, latitude, longitude, null);
    }

    public long tenderAmount() { return tendersToUse == null ? 0 : tendersToUse; }
}
