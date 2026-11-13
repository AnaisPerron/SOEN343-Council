package com.council.tenderapp.technicalservices.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Lets Hibernate create the transaction table on development databases. */
@Entity
@Table(name = "tender_cash_transactions", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "reason"}))
public class TenderCashTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(nullable = false, length = 24)
    private String reason;
    @Column(nullable = false)
    private Long tenders;
}
