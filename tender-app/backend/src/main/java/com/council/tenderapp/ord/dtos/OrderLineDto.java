package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;

public record OrderLineDto(
    Long menuItemId,
    String name,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {}
