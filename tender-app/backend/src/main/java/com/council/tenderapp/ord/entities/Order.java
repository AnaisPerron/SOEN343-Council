package com.council.tenderapp.ord.entities;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    public static final String STATUS_PLACED = "placed";

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    protected Order() {
    }

    public Order(Long customerId, Long restaurantId, String status, Instant createdAt) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.status = status;
        this.createdAt = createdAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
