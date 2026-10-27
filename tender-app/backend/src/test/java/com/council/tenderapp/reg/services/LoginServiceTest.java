package com.council.tenderapp.reg.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.LoginResponse;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.exceptions.InvalidCredentialsException;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.security.Session;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private ProfileRepository repository;

    @Mock
    private RestaurantRepository restaurantRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private SessionService sessionService;
    private LoginService service;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(Duration.ofHours(1));
        service = new LoginService(repository, restaurantRepository, passwordEncoder, sessionService);
    }

    private Profile profile(String password, Role role) {
        Profile profile = new Profile(role, "Jane", "jane@example.com", passwordEncoder.encode(password), "514-555-0101");
        ReflectionTestUtils.setField(profile, "id", 7L);
        return profile;
    }

    @Test
    void issuesSessionWithAccountIdAndRole() {
        when(repository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(profile("secret123", Role.COURIER)));

        LoginResponse response = service.login(new LoginRequest(" Jane@Example.com ", "secret123"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.role()).isEqualTo(Role.COURIER);
        Session session = sessionService.find(response.token()).orElseThrow();
        assertThat(session.accountId()).isEqualTo(7L);
        assertThat(session.role()).isEqualTo(Role.COURIER);
    }

    @Test
    void rejectsWrongPassword() {
        when(repository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(profile("secret123", Role.CUSTOMER)));

        assertThatThrownBy(() -> service.login(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void rejectsUnknownEmailWithSameMessage() {
        when(repository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@example.com", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void logoutInvalidatesSession() {
        when(repository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(profile("secret123", Role.CUSTOMER)));
        String token = service.login(new LoginRequest("jane@example.com", "secret123")).token();

        service.logout(token);

        assertThat(sessionService.find(token)).isEmpty();
    }

    @Test
    void acceptsSeededDemoPasswordHash() {
        // Hash used for every account in demo/seed.sql
        String seededHash = "$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi";
        assertThat(passwordEncoder.matches("DemoPass123!", seededHash)).isTrue();
    }
}
