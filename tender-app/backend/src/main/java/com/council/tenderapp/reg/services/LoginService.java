package com.council.tenderapp.reg.services;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    // Checked against when the email is unknown so both failures take about as long
    private final String dummyHash;

    public LoginService(ProfileRepository repository, RestaurantRepository restaurantRepository, PasswordEncoder passwordEncoder, SessionService sessionService) {
        this.repository = repository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    @Transactional(readOnly = true)
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

        if (profile.getRole() == Role.STAFF && profile.getName() != null) {

            String rawName = profile.getName();
            String cleanRestaurantName = rawName.endsWith(" Staff") 
                ? rawName.substring(0, rawName.length() - 6).trim() 
                : rawName;

            Restaurant restaurant = restaurantRepository.findByName(cleanRestaurantName)
                .orElseThrow(() -> new RuntimeException("Associated restaurant not found for name: " + cleanRestaurantName));
        
            restaurantId = restaurant.getId();
        }

        Session session = sessionService.create(profile.getId(), restaurantId, profile.getRole());
        
        return LoginResponse.from(session, profile);
    }

    public void logout(String token) {
        sessionService.invalidate(token);
    }
}
