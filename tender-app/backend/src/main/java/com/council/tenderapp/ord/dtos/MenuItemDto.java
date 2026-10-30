package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;

import com.council.tenderapp.ord.entities.MenuItem;

public record MenuItemDto(
    Long id,
    Long menu_id,
    String name,
    BigDecimal price,
    String description,
    Boolean available,
    String photoUrl
) {
    public MenuItemDto(MenuItem menuItem) {
        this(
            menuItem.getId(),
            menuItem.getMenuId(),
            menuItem.getName(),
            menuItem.getPrice(),
            menuItem.getDescription(),
            "available".equals(menuItem.getStatus()),
            menuItem.getPhotoUrl()
        );
    }
}
