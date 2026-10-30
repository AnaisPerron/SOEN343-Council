package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;
import java.util.List;

// unitPrice includes the selected add-ons, so unitPrice * quantity is what the customer paid for the line.
public record CustomerOrderLineDto(
    String name,
    Integer quantity,
    BigDecimal unitPrice,
    List<String> options
) {}
