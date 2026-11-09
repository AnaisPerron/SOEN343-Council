package com.council.tenderapp.ord.dto.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.council.tenderapp.ord.entities.OrderStatus;

public class OrderResponse {

    private final Long id;
    private final Long restaurantId;
    private final Long courierId;
    private final OrderStatus status;
    private final Date dateOrdered;
    private final Date acceptedAt;
    private final String rejectionReason;
    private final BigDecimal totalPrice;
    private final String customerName;
    private final String customerPhone;
    private final String deliveryAddress;
    private final List<OrderLineResponse> orderLines;

    public OrderResponse(
        Long id,
        Long restaurantId,
        Long courierId,
        OrderStatus status,
        Date dateOrdered,
        Date acceptedAt,
        String rejectionReason,
        BigDecimal totalPrice,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        List<OrderLineResponse> orderLines
    ) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.courierId = courierId;
        this.status = status;
        this.dateOrdered = dateOrdered == null ? null : new Date(dateOrdered.getTime());
        this.acceptedAt = acceptedAt == null ? null : new Date(acceptedAt.getTime());
        this.rejectionReason = rejectionReason;
        this.totalPrice = totalPrice;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.orderLines = List.copyOf(orderLines);
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() { return restaurantId; }

    public Long getCourierId() { return courierId; }

    public OrderStatus getStatus() {
        return status;
    }

    public Date getDateOrdered() {
        return dateOrdered == null ? null : new Date(dateOrdered.getTime());
    }

    public Date getAcceptedAt() {
        return acceptedAt == null ? null : new Date(acceptedAt.getTime());
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public List<OrderLineResponse> getOrderLines() {
        return orderLines;
    }
}
