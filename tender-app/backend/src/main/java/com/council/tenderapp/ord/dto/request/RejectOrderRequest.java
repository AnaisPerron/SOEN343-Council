package com.council.tenderapp.ord.dto.request;

// A restaurant may reject an order without providing a reason.
public record RejectOrderRequest(String reason) {}
