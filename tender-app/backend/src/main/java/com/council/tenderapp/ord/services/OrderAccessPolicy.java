package com.council.tenderapp.ord.services;

import java.util.Objects;

import com.council.tenderapp.ord.entities.Order;

/**
 * The one place that decides which customer, restaurant or courier an order belongs to.
 * Callers keep their own response to a refusal (status code, exception type, message).
 */
public final class OrderAccessPolicy {
    private OrderAccessPolicy() {}

    /** The customer who placed the order; an unknown (null) customer never matches. */
    public static boolean isPlacedBy(Order order, Long customerId) {
        return customerId != null && customerId.equals(order.getCustomerId());
    }

    /** The courier the order is assigned to. */
    public static boolean isAssignedTo(Order order, Long courierId) {
        return Objects.equals(order.getCourierId(), courierId);
    }

    /** The restaurant the order was placed at. */
    public static boolean isFromRestaurant(Order order, Long restaurantId) {
        return Objects.equals(order.getRestaurantId(), restaurantId);
    }
}
