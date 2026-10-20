package com.council.tenderapp.ORD.persistence;

import java.util.Date;

public class Order {
    
    private Long id;

    private Date dateOrdered;

    private Date pickupDate;

    private Date dateDelivered;

    private OrderStatus status;

    private double totalPrice;

    public Order() {
    }

    public Order(Date dateOrdered, Date pickupDate, Date dateDelivered, OrderStatus status, double totalPrice) {
        this.dateOrdered = dateOrdered;
        this.pickupDate = pickupDate;
        this.dateDelivered = dateDelivered;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        this.status = status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
