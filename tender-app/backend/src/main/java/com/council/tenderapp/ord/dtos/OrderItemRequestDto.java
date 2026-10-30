package com.council.tenderapp.ord.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequestDto(
    @NotNull(message = "Menu item id is required")
    Long menuItemId,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    Integer quantity
) {}
