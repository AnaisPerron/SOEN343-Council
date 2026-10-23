package com.council.tenderapp.reg.services;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
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

    public RegistrationService(ProfileRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
            return RegisterResponse.from(repository.saveAndFlush(profile));
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
