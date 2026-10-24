package com.council.tenderapp.ord.exceptions;

public class MenuItemUnavailableException extends RuntimeException {
    public MenuItemUnavailableException(String message) {
        super(message);
    }
}
