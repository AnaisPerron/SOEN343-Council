package com.council.tenderapp.ord.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequestDto(
    @NotNull(message = "Restaurant id is required")
    Long restaurantId,

    @NotEmpty(message = "Order must contain at least one item")
    List<@Valid @NotNull(message = "Order item is required") OrderItemRequestDto> items
) {}
