package com.council.tenderapp.reg.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.exceptions.DuplicateEmailException;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.ProfileRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private ProfileRepository repository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(repository, passwordEncoder);
    }

    @Test
    void registersProfileWithHashedPassword() {
        when(repository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResponse response = service.register(
                new RegisterRequest("Jane", " Jane@Example.com ", "secret123", "514-555-0101", Role.CUSTOMER));

        ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(ReflectionTestUtils.getField(saved.getValue(), "role")).isEqualTo("customer");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void rejectsDuplicateEmail() {
        when(repository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Jane", "jane@example.com", "secret123", "514-555-0101", Role.COURIER)))
                .isInstanceOf(DuplicateEmailException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void translatesUniqueConstraintViolationToDuplicateEmail() {
        when(repository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Profile.class))).thenThrow(
                new DataIntegrityViolationException("unique", new SQLException("duplicate key", "23505")));

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Jane", "jane@example.com", "secret123", "514-555-0101", Role.STAFF)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void doesNotReportOtherConstraintViolationsAsDuplicateEmail() {
        when(repository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Profile.class))).thenThrow(
                new DataIntegrityViolationException("check", new SQLException("check violation", "23514")));

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Jane", "jane@example.com", "secret123", "514-555-0101", Role.CUSTOMER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
