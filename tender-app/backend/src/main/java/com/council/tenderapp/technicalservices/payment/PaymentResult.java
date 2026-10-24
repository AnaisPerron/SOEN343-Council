package com.council.tenderapp.technicalservices.payment;

public record PaymentResult(boolean approved, String reference, String message) {

    public static PaymentResult approved(String reference) {
        return new PaymentResult(true, reference, "Approved");
    }

    public static PaymentResult declined(String message) {
        return new PaymentResult(false, null, message);
    }
}
