package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;

public record MenuItemDto(
    Long id,
    Long menu_id,
    String name,
    BigDecimal price,
    String description,
    Boolean available,
    String photoUrl
) {}
