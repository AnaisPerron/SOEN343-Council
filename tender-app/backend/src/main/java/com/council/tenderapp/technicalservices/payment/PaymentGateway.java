package com.council.tenderapp.technicalservices.payment;

import java.math.BigDecimal;

// Billing system boundary; swap the implementation for a real provider in a later phase
public interface PaymentGateway {

    PaymentResult charge(Long customerId, BigDecimal amount);
}
