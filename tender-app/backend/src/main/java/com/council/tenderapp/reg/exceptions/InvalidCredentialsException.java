package com.council.tenderapp.reg.exceptions;

public class InvalidCredentialsException extends RuntimeException {

    // Same message for unknown email and wrong password so accounts cannot be enumerated
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
