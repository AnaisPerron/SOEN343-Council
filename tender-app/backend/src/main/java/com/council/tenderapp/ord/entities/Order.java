package com.council.tenderapp.ord.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.persistence.Convert;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "created_at", nullable = false)
    private Date dateOrdered;

    @Column(name = "pickup_date")
    private Date pickupDate;

    @Column(name = "date_delivered")
    private Date dateDelivered;

    @Convert(converter = OrderStatusConverter.class)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private OrderStatus status;

    @Column(name = "accepted_at")
    private Date acceptedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();

    public Order() {
    }

    public Order(Long restaurantId, Date dateOrdered, OrderStatus status) {
        this.restaurantId = restaurantId;
        this.dateOrdered = dateOrdered == null ? null : new Date(dateOrdered.getTime());
        this.status = status;
    }

    public Order(Long customerId, Long restaurantId, OrderStatus status, BigDecimal totalPrice) {
        this(restaurantId, new Date(), status);
        this.customerId = customerId;
        this.totalPrice = totalPrice;
    }

    public Long getCustomerId() { return customerId; }
    public Long getCourierId() { return courierId; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Date getDateOrdered() {
        return dateOrdered == null ? null : new Date(dateOrdered.getTime());
    }

    public void setDateOrdered(Date dateOrdered) {
        this.dateOrdered = dateOrdered == null ? null : new Date(dateOrdered.getTime());
    }

    public Date getPickupDate() {
        return pickupDate == null ? null : new Date(pickupDate.getTime());
    }

    public void setPickupDate(Date pickupDate) {
        this.pickupDate = pickupDate == null ? null : new Date(pickupDate.getTime());
    }

    public Date getDateDelivered() {
        return dateDelivered == null ? null : new Date(dateDelivered.getTime());
    }

    public void setDateDelivered(Date dateDelivered) {
        this.dateDelivered = dateDelivered == null ? null : new Date(dateDelivered.getTime());
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        transitionTo(status);
    }

    public void transitionTo(OrderStatus nextStatus) {
        if (status == null) {
            throw new IllegalStateException("Cannot transition an order without a current status");
        }
        OrderStatus transitionedStatus = status.transitionTo(nextStatus);
        if (transitionedStatus == OrderStatus.ACCEPTED && acceptedAt == null) {
            acceptedAt = new Date();
        }
        this.status = transitionedStatus;
    }

    public Date getAcceptedAt() {
        return acceptedAt == null ? null : new Date(acceptedAt.getTime());
    }

    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt == null ? null : new Date(acceptedAt.getTime());
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public BigDecimal getTotalPrice() {
        // The stored total includes add-on costs, which order lines do not carry.
        if (totalPrice != null) {
            return totalPrice;
        }
        if (orderLines == null || orderLines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return orderLines.stream()
            .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLine> orderLines) {
        this.orderLines.clear();
        if (orderLines == null) {
            return;
        }
        for (OrderLine orderLine : orderLines) {
            addOrderLine(orderLine);
        }
    }

    public void addOrderLine(OrderLine orderLine) {
        if (orderLine == null) {
            return;
        }
        orderLines.add(orderLine);
        orderLine.setOrder(this);
    }

    public void removeOrderLine(OrderLine orderLine) {
        if (orderLine == null) {
            return;
        }
        orderLines.remove(orderLine);
        orderLine.setOrder(null);
    }

    @PrePersist
    @PreUpdate
    private void ensureAcceptanceTime() {
        if (status == OrderStatus.ACCEPTED && acceptedAt == null) {
            acceptedAt = new Date();
        }
    }
}
