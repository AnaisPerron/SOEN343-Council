package com.council.tenderapp.dto;

import com.council.tenderapp.model.Profile;
import com.council.tenderapp.model.Role;

public record RegisterResponse(Long id, String name, String email, String phone, Role role) {

    public static RegisterResponse from(Profile profile) {
        return new RegisterResponse(
                profile.getId(),
                profile.getName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getRole());
    }
}
