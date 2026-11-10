package com.council.tenderapp.dlv.dtos;

import java.math.BigDecimal;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.Restaurant;

/** The order a courier is currently delivering, with where to pick it up and where to drop it off. */
public record AssignedOrderDto(
    Long orderId,
    String status,
    String restaurantName,
    String restaurantAddress,
    String deliveryAddress,
    BigDecimal totalPrice
) {
    public AssignedOrderDto(Order order, Restaurant restaurant) {
        this(
            order.getId(),
            order.getStatus().name().toLowerCase(),
            restaurant == null ? null : restaurant.getName(),
            restaurant == null ? null : restaurant.getAddress(),
            order.getDeliveryAddress(),
            order.getTotalPrice()
        );
    }
}
