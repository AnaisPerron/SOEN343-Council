package com.council.tenderapp.ord.dtos;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequestDto(
    @NotNull(message = "Menu item id is required")
    Long menuItemId,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    Integer quantity,

    // Selected add-ons for this item; omitted or empty means none.
    List<@NotNull(message = "Option id is required") Long> optionIds
) {
    public OrderItemRequestDto(Long menuItemId, Integer quantity) {
        this(menuItemId, quantity, List.of());
    }

    public List<Long> selectedOptionIds() {
        return optionIds == null ? List.of() : optionIds.stream().distinct().toList();
    }
}
