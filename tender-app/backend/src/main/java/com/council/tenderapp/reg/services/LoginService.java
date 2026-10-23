package com.council.tenderapp.reg.services;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.LoginResponse;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.exceptions.InvalidCredentialsException;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.reg.security.Session;

@Service
public class LoginService {

    private final ProfileRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    // Checked against when the email is unknown so both failures take about as long
    private final String dummyHash;

    public LoginService(ProfileRepository repository, PasswordEncoder passwordEncoder, SessionService sessionService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Profile profile = repository.findByEmailIgnoreCase(email).orElse(null);

        if (profile == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.password(), profile.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Session session = sessionService.create(profile.getId(), profile.getRole());
        return LoginResponse.from(session, profile);
    }

    public void logout(String token) {
        sessionService.invalidate(token);
    }
}
