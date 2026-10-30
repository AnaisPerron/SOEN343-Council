package com.council.tenderapp.reg.dtos;

import com.council.tenderapp.reg.entities.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Phone must be a valid phone number")
        String phone,

        @NotNull(message = "Role is required")
        Role role,
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address) {

    public RegisterRequest(String name, String email, String password, String phone, Role role) {
        this(name, email, password, phone, role, null);
    }
}
