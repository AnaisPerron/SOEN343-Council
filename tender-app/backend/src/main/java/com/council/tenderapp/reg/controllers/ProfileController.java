package com.council.tenderapp.reg.controllers;

import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.dtos.UpdateProfileRequest;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.entities.RestaurantStaff;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.repositories.RestaurantStaffRepository;
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
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileRepository profiles;
    private final RestaurantRepository restaurants;
    private final RestaurantStaffRepository restaurantStaff;

    public ProfileController(ProfileRepository profiles, RestaurantRepository restaurants,
            RestaurantStaffRepository restaurantStaff) {
        this.profiles = profiles;
        this.restaurants = restaurants;
        this.restaurantStaff = restaurantStaff;
    }

    @GetMapping
    public RegisterResponse get(Authentication authentication) {
        Profile profile = currentProfile(authentication);
        return profile.getRole() == Role.STAFF
                ? RegisterResponse.from(profile, currentRestaurant(profile))
                : RegisterResponse.from(profile);
    }

    @Transactional
    @PatchMapping
    public RegisterResponse update(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        Profile profile = currentProfile(authentication);
        profile.updateContact(request.name(), request.phone());
        if (profile.getRole() == Role.STAFF) {
            Restaurant restaurant = currentRestaurant(profile);
            restaurant.setName(request.name().trim());
            restaurant.setPhone(request.phone().trim());
            if (request.address() != null) restaurant.setAddress(request.address().trim());
            restaurants.save(restaurant);
            profiles.save(profile);
            return RegisterResponse.from(profile, restaurant);
        }
        return RegisterResponse.from(profiles.save(profile));
    }

    private Profile currentProfile(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long accountId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return profiles.findById(accountId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Restaurant currentRestaurant(Profile profile) {
        RestaurantStaff staff = restaurantStaff.findById(profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Restaurant link unavailable"));
        return restaurants.findById(staff.getRestaurantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Restaurant unavailable"));
    }
}
