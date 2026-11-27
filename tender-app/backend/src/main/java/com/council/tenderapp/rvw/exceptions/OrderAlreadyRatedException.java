package com.council.tenderapp.rvw.exceptions;

public class OrderAlreadyRatedException extends RuntimeException {
    public OrderAlreadyRatedException(Long orderId) {
        super("Order " + orderId + " has already been rated");
    }
}
