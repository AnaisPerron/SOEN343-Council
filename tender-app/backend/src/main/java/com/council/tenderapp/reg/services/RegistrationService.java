package com.council.tenderapp.reg.services;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.exceptions.DuplicateEmailException;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.repositories.ProfileRepository;

@Service
public class RegistrationService {

    private static final String UNIQUE_VIOLATION = "23505";

    private final ProfileRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public RegistrationService(ProfileRepository repository, PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        Profile profile = new Profile(
                request.role(),
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone().trim());

        try {
            Profile saved = repository.saveAndFlush(profile);
            if (request.role() == com.council.tenderapp.reg.entities.Role.STAFF) {
                Long restaurantId = jdbc.queryForObject("insert into restaurants (name, status) values (?, 'closed') returning id", Long.class, request.name().trim());
                jdbc.update("insert into restaurant_staff (user_id, restaurant_id) values (?, ?)", saved.getId(), restaurantId);
                jdbc.update("insert into menus (restaurant_id, name) values (?, ?)", restaurantId, request.name().trim() + " Menu");
            }
            return RegisterResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Another request registered the same email between the check and the insert
            if (isUniqueViolation(ex)) {
                throw new DuplicateEmailException(email);
            }
            throw ex;
        }
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        return NestedExceptionUtils.getMostSpecificCause(ex) instanceof SQLException sqlEx
                && UNIQUE_VIOLATION.equals(sqlEx.getSQLState());
    }
}
