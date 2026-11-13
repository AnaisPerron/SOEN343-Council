package com.council.tenderapp.dlv.entities;

import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.*;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.reg.entities.CourierProfile;

@Entity
@Table(name = "assignments")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "courier_id", nullable = false)
    private Long courierId;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    @Convert(converter = AssignmentStatusConverter.class)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private CourierProfile courier;

    protected Assignment() {}

    public Assignment(Long orderId, Long courierId, Instant assignedAt) {
        this.orderId = Objects.requireNonNull(orderId);
        this.courierId = Objects.requireNonNull(courierId);
        this.assignedAt = Objects.requireNonNull(assignedAt).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.status = AssignmentStatus.OFFERED;
    }

    public void accept(Instant at) {
        if (status == AssignmentStatus.ACCEPTED) return;
        if (status != AssignmentStatus.OFFERED) throw new IllegalStateException("Assignment is no longer offered");
        if (at == null || at.isBefore(assignedAt)) throw new IllegalArgumentException("Acceptance precedes the offer");
        acceptedAt = at.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        status = AssignmentStatus.ACCEPTED;
    }

    public void complete() {
        if (status != AssignmentStatus.ACCEPTED) throw new IllegalStateException("Assignment must be accepted before completion");
        status = AssignmentStatus.COMPLETED;
    }

    public void end() {
        if (!status.isActive()) throw new IllegalStateException("Assignment is no longer active");
        status = AssignmentStatus.ENDED;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getCourierId() { return courierId; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public AssignmentStatus getStatus() { return status; }
}
