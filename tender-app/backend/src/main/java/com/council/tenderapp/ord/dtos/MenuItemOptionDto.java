package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;

public record MenuItemOptionDto(
    Long id,
    Long menu_item_id,
    String name,
    BigDecimal additional_cost
) {}
