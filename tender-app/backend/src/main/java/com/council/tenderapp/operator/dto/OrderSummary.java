package com.council.tenderapp.operator.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummary(
        long id,
        Long customerId,
        Long restaurantId,
        Long courierId,
        String status,
        Instant dateOrdered,
        Instant acceptedAt,
        String rejectionReason,
        BigDecimal totalPrice) {}
