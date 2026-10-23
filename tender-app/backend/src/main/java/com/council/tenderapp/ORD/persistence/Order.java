package com.council.tenderapp.ORD.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_ordered")
    private Date dateOrdered;

    @Temporal(TemporalType.TIMESTAMP)
    private Date pickupDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateDelivered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_price")
    private Double totalPrice;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "accepted_at")
    private Date acceptedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Order(Date dateOrdered, Date pickupDate, Date dateDelivered, OrderStatus status, double totalPrice) {
        this.dateOrdered = dateOrdered;
        this.pickupDate = pickupDate;
        this.dateDelivered = dateDelivered;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public Order(Long restaurantId, Date dateOrdered, OrderStatus status, double totalPrice) {
        this.restaurantId = restaurantId;
        this.dateOrdered = dateOrdered;
        setStatus(status);
        this.totalPrice = totalPrice;
    }

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
        return dateOrdered;
    }

    public void setDateOrdered(Date dateOrdered) {
        this.dateOrdered = dateOrdered;
    }

    public Date getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(Date pickupDate) {
        this.pickupDate = pickupDate;
    }

    public Date getDateDelivered() {
        return dateDelivered;
    }

    public void setDateDelivered(Date dateDelivered) {
        this.dateDelivered = dateDelivered;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        if (status == OrderStatus.Accepted && this.status != OrderStatus.Accepted && acceptedAt == null) {
            acceptedAt = new Date();
        }
        this.status = status;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Date getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        List<OrderItem> replacementItems = new ArrayList<>(items);
        this.items.clear();
        for (OrderItem item : replacementItems) {
            addItem(item);
        }
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    @PrePersist
    @PreUpdate
    private void ensureAcceptanceTime() {
        if (status == OrderStatus.Accepted && acceptedAt == null) {
            acceptedAt = new Date();
        }
    }
}
