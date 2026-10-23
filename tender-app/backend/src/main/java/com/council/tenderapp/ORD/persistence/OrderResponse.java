package com.council.tenderapp.ORD.persistence;

import java.util.Date;
import java.util.List;

public class OrderResponse {

    private final Long id;
    private final OrderStatus status;
    private final Date acceptedAt;
    private final List<OrderItemResponse> items;

    public OrderResponse(Long id, OrderStatus status, Date acceptedAt, List<OrderItemResponse> items) {
        this.id = id;
        this.status = status;
        this.acceptedAt = acceptedAt == null ? null : new Date(acceptedAt.getTime());
        this.items = List.copyOf(items);
    }

    public Long getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Date getAcceptedAt() {
        return acceptedAt == null ? null : new Date(acceptedAt.getTime());
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }
}
