package com.council.tenderapp.ORD.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectOrderRequest(@NotBlank String reason) {}
