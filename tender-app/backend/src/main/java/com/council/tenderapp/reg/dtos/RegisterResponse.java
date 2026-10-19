package com.council.tenderapp.reg.dtos;

import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;

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
