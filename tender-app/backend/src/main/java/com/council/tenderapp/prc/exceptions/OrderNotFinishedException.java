package com.council.tenderapp.prc.exceptions;

public class OrderNotFinishedException extends RuntimeException {
    public OrderNotFinishedException(String message) {
        super(message);
    }
    
}
