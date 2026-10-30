package com.council.tenderapp.operator.dto;

import jakarta.validation.constraints.NotNull;

public record RestaurantStatusRequest(
        @NotNull(message = "Status is required") RestaurantStatus status) {}
