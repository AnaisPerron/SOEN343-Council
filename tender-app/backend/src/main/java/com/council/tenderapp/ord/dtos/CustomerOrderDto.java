package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// One entry in a customer's order history.
public record CustomerOrderDto(
    Long orderId,
    Long restaurantId,
    String restaurantName,
    String status,
    BigDecimal total,
    Instant placedAt,
    String rejectionReason,
    List<CustomerOrderLineDto> lines
) {}
