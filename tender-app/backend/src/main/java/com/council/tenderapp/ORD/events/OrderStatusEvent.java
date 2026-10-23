package com.council.tenderapp.ORD.events;

import com.council.tenderapp.ORD.entities.OrderStatus;
import java.time.Instant;

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
