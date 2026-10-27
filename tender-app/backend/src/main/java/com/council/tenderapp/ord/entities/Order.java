package com.council.tenderapp.ord.entities;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    public static final String STATUS_PLACED = "placed";
    public static final String STATUS_CANCELLED = "cancelled";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    protected Order() {
    }

    public Order(Long customerId, Long restaurantId, String status, BigDecimal totalPrice) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public Long getCourierId() {
        return courierId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
