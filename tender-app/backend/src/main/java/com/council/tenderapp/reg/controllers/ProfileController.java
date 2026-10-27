package com.council.tenderapp.reg.controllers;

import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.dtos.UpdateProfileRequest;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileRepository profiles;

    public ProfileController(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public RegisterResponse get(Authentication authentication) {
        return RegisterResponse.from(currentProfile(authentication));
    }

    @PatchMapping
    public RegisterResponse update(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        Profile profile = currentProfile(authentication);
        profile.updateContact(request.name(), request.phone());
        return RegisterResponse.from(profiles.save(profile));
    }

    private Profile currentProfile(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long accountId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return profiles.findById(accountId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
