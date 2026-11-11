package com.council.tenderapp.prc.entities;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity 
@Table(name = "ledger_entries", schema = "public")
public class LedgerEntry 
{
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @PrePersist
    void ensureTimestamp() {
        if (timestamp == null) timestamp = OffsetDateTime.now();
    }

    @Column(name = "final_order_status", nullable = false)
    private String finalOrderStatus;

    @Column(name = "total_charge")
    private Double totalCharge;

    public LedgerEntry() {
    }

    public LedgerEntry(Long id, Long billId, String transactionType, OffsetDateTime timestamp,
            String finalOrderStatus, Double totalCharge) {
        this.id = id;
        this.billId = billId;
        this.transactionType = transactionType;
        this.timestamp = timestamp;
        this.finalOrderStatus = finalOrderStatus;
        this.totalCharge = totalCharge;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    
    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }

    public String getFinalOrderStatus() {
        return finalOrderStatus;
    }

    public void setFinalOrderStatus(String finalOrderStatus) {
        this.finalOrderStatus = finalOrderStatus;
    }

    
}
