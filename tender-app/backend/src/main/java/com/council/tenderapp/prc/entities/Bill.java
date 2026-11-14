package com.council.tenderapp.prc.entities;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Bill
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "date")
    private OffsetDateTime date;

    @Column(name = "service_fee")
    private Double serviceFee;

    @Column(name = "delivery_fee")
    private Double deliveryFee;

    @Column(name = "small_order_fee")
    private Double smallOrderFee;

    public Bill() {
    }

    public Bill(Long id, Long orderId, Long customerId, Double amount, OffsetDateTime date, Double serviceFee,
            Double deliveryFee, Double smallOrderFee) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.date = date;
        this.serviceFee = serviceFee;
        this.deliveryFee = deliveryFee;
        this.smallOrderFee = smallOrderFee;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public Double getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(Double serviceFee) {
        this.serviceFee = serviceFee;
    }

    public Double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public Double getSmallOrderFee() {
        return smallOrderFee;
    }

    public void setSmallOrderFee(Double smallOrderFee) {
        this.smallOrderFee = smallOrderFee;
    }


}
