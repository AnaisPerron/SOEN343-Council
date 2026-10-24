package com.council.tenderapp.ord.dtos;

import java.util.List;

public record MenuResponseDTO (
    List<MenuItemDto> menuItems,
    List<MenuItemOptionDto> menuItemOptions
) {}
