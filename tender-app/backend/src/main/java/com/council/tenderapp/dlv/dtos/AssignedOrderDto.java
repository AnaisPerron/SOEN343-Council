package com.council.tenderapp.dlv.dtos;

import java.time.Instant;
import com.council.tenderapp.dlv.entities.Assignment;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.Restaurant;

public record AssignedOrderDto(
    Long assignmentId,
    String assignmentStatus,
    Instant assignedAt,
    Instant acceptedAt,
    Long orderId,
    String status,
    String restaurantName,
    String restaurantAddress,
    String deliveryAddress
) {
    public AssignedOrderDto(Order order, Restaurant restaurant, Assignment assignment) {
        this(assignment.getId(), assignment.getStatus().value(), assignment.getAssignedAt(), assignment.getAcceptedAt(),
                order.getId(), order.getStatus().name().toLowerCase(), restaurant == null ? null : restaurant.getName(),
                restaurant == null ? null : restaurant.getAddress(), order.getDeliveryAddress());
    }
}
