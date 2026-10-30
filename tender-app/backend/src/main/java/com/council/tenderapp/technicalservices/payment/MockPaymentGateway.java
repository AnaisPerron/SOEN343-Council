package com.council.tenderapp.technicalservices.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.council.tenderapp.technicalservices.logging.AppLogService;

// Simulated payments for this phase: every positive amount is approved, no money moves
@Service
public class MockPaymentGateway implements PaymentGateway {

    private final AppLogService logService;

    public MockPaymentGateway(AppLogService logService) {
        this.logService = logService;
    }

    @Override
    public PaymentResult charge(Long customerId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return PaymentResult.declined("Amount must be greater than 0");
        }

        String reference = "MOCK-" + UUID.randomUUID();
        logService.info("Mock payment approved: customer " + customerId + " charged " + amount + " (" + reference + ")");
        return PaymentResult.approved(reference);
    }
}
