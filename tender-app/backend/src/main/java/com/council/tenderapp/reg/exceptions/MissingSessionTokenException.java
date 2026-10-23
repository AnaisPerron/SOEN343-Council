package com.council.tenderapp.reg.exceptions;

public class MissingSessionTokenException extends RuntimeException {

    public MissingSessionTokenException() {
        super("Missing or malformed Authorization header, expected 'Bearer <token>'");
    }
}
