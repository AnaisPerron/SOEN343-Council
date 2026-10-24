package com.council.tenderapp.ord.dtos;

import java.time.Instant;

public record OrderStatusResponseDto(
    Long orderId,
    String previousStatus,
    String status,
    Instant changedAt
) {}
