package com.council.tenderapp.ord.dtos;

// A restaurant may reject an order without providing a reason.
public record RejectOrderRequest(String reason) {}
