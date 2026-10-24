package com.council.tenderapp.ord.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MenuItemRequestDto(
    @NotBlank(message = "Item name is required")
    String name,
    
    String description,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price cannot be negative")
    BigDecimal price,
    
    boolean available
) {}