package com.council.tenderapp.reg.dtos;

import java.time.Instant;

import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.models.Session;

public record LoginResponse(String token, Instant expiresAt, Long id, String name, String email, String phone, Role role) {

    public static LoginResponse from(Session session, Profile profile) {
        return new LoginResponse(
                session.token(),
                session.expiresAt(),
                profile.getId(),
                profile.getName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getRole());
    }
}
