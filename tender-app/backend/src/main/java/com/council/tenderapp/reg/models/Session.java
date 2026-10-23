package com.council.tenderapp.reg.models;

import java.time.Instant;

import com.council.tenderapp.reg.entities.Role;

public record Session(String token, Long accountId, Role role, Instant expiresAt) {

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
