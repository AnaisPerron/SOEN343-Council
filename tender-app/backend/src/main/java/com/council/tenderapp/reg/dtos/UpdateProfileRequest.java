package com.council.tenderapp.reg.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Phone must be a valid phone number")
        String phone,
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address) {

    public UpdateProfileRequest(String name, String phone) {
        this(name, phone, null);
    }
}
