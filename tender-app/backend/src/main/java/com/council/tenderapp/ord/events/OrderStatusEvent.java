package com.council.tenderapp.ord.events;

import java.time.Instant;

import com.council.tenderapp.ord.entities.OrderStatus;

public record OrderStatusEvent(
    String eventId,
    Long orderId,
    Long restaurantId,
    OrderStatus previousStatus,
    OrderStatus newStatus,
    String action,
    Instant occurredAt,
    String rejectionReason
) {
}
