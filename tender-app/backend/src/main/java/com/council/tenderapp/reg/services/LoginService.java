package com.council.tenderapp.reg.services;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.entities.RestaurantStaff;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.repositories.RestaurantStaffRepository;
import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.LoginResponse;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.exceptions.InvalidCredentialsException;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.reg.security.Session;

@Service
public class LoginService {

    private final ProfileRepository repository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    // Checked against when the email is unknown so both failures take about as long
    private final String dummyHash;

    public LoginService(ProfileRepository repository, RestaurantRepository restaurantRepository,
            RestaurantStaffRepository restaurantStaffRepository, PasswordEncoder passwordEncoder,
            SessionService sessionService) {
        this.repository = repository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Profile profile = repository.findByEmailIgnoreCase(email).orElse(null);
        Long restaurantId = null;

        if (profile == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.password(), profile.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (profile.getRole() == Role.STAFF) {
            RestaurantStaff staff = restaurantStaffRepository.findById(profile.getId()).orElse(null);
            if (staff == null) {
                // Accounts created before restaurant linking was added get their own closed restaurant.
                // Never claim an existing restaurant by matching a mutable display name.
                Restaurant restaurant = restaurantRepository.saveAndFlush(
                        new Restaurant(profile.getName(), "closed", null, null));
                staff = restaurantStaffRepository.saveAndFlush(new RestaurantStaff(profile.getId(), restaurant.getId()));
            }
            restaurantId = staff.getRestaurantId();
        }

        Session session = sessionService.create(profile.getId(), restaurantId, profile.getRole());
        
        return LoginResponse.from(session, profile);
    }

    public void logout(String token) {
        sessionService.invalidate(token);
    }
}
