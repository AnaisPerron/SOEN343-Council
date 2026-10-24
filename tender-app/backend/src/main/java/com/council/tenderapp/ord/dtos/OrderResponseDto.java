package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDto(
    Long orderId,
    Long restaurantId,
    String status,
    BigDecimal total,
    Instant createdAt,
    String paymentReference,
    List<OrderLineDto> lines
) {}
