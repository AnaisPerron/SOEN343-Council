package com.council.tenderapp.reg.dtos;

import java.time.Instant;

import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.security.Session;
import com.council.tenderapp.ord.entities.Restaurant;

public record LoginResponse(String token, Instant expiresAt, Long id, String name, String email,
        String phone, Role role, Long restaurantId, String address) {

    public LoginResponse(String token, Instant expiresAt, Long id, String name, String email,
            String phone, Role role) {
        this(token, expiresAt, id, name, email, phone, role, null, null);
    }

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

    public static LoginResponse from(Session session, Profile profile, Restaurant restaurant) {
        return new LoginResponse(session.token(), session.expiresAt(), profile.getId(),
                restaurant.getName(), profile.getEmail(),
                restaurant.getPhone() == null ? profile.getPhone() : restaurant.getPhone(), profile.getRole(),
                restaurant.getId(), restaurant.getAddress());
    }

    public LoginResponse withAddress(String customerAddress) {
        return new LoginResponse(token, expiresAt, id, name, email, phone, role, restaurantId, customerAddress);
    }
}
