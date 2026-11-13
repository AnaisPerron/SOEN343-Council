package com.council.tenderapp.operator.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReassignOrderRequest(@NotNull @Positive Long expectedCourierId) {}
