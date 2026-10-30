package com.council.tenderapp.reg.dtos;

import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.ord.entities.Restaurant;

public record RegisterResponse(Long id, String name, String email, String phone, Role role,
        Long restaurantId, String address) {

    public RegisterResponse(Long id, String name, String email, String phone, Role role) {
        this(id, name, email, phone, role, null, null);
    }

    public static RegisterResponse from(Profile profile) {
        return new RegisterResponse(
                profile.getId(),
                profile.getName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getRole());
    }

    public static RegisterResponse from(Profile profile, Restaurant restaurant) {
        return new RegisterResponse(profile.getId(), restaurant.getName(), profile.getEmail(),
                restaurant.getPhone() == null ? profile.getPhone() : restaurant.getPhone(),
                profile.getRole(), restaurant.getId(), restaurant.getAddress());
    }
}
