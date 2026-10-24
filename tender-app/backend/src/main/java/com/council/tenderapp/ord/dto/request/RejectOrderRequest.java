package com.council.tenderapp.ord.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectOrderRequest(@NotBlank String reason) {}
