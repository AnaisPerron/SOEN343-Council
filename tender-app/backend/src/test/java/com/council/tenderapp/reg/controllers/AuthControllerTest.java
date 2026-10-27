package com.council.tenderapp.reg.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.council.tenderapp.reg.dtos.LoginResponse;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.exceptions.DuplicateEmailException;
import com.council.tenderapp.reg.exceptions.InvalidCredentialsException;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.security.Session;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;
import com.council.tenderapp.reg.services.SessionService;
import com.council.tenderapp.reg.repositories.ProfileRepository;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private ProfileRepository profiles;

    private static final String VALID_BODY = """
            {"name":"Jane","email":"jane@example.com","password":"secret123","phone":"514-555-0101","role":"CUSTOMER"}
            """;

    @Test
    void returnsCreatedWithoutPassword() throws Exception {
        when(registrationService.register(any()))
                .thenReturn(new RegisterResponse(1L, "Jane", "jane@example.com", "514-555-0101", Role.CUSTOMER));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsMissingFieldsAndInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"not-an-email","role":"CUSTOMER"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid email address"))
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
        verifyNoInteractions(registrationService);
    }

    @Test
    void acceptsLowercaseRole() throws Exception {
        when(registrationService.register(any()))
                .thenReturn(new RegisterResponse(1L, "Jane", "jane@example.com", "514-555-0101", Role.STAFF));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("CUSTOMER", "staff")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsUnknownRole() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("CUSTOMER", "ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(registrationService);
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        when(registrationService.register(any())).thenThrow(new DuplicateEmailException("jane@example.com"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with email jane@example.com already exists"));
    }

    private static final String LOGIN_BODY = """
            {"email":"jane@example.com","password":"secret123"}
            """;

    @Test
    void loginReturnsTokenWithAccountIdAndRole() throws Exception {
        when(loginService.login(any())).thenReturn(new LoginResponse(
                "abc123", Instant.parse("2026-01-02T00:00:00Z"), 1L, "Jane", "jane@example.com", "514-555-0101",
                Role.CUSTOMER));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc123"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginReturnsUnauthorizedForBadCredentials() throws Exception {
        when(loginService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email is required"))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));
        verifyNoInteractions(loginService);
    }

    @Test
    void restoresAnActiveSession() throws Exception {
        Session session = new Session("abc123", 1L, null, Role.CUSTOMER, Instant.parse("2026-01-02T00:00:00Z"));
        when(sessionService.find("abc123")).thenReturn(Optional.of(session));
        when(profiles.findById(1L)).thenReturn(Optional.of(new Profile(Role.CUSTOMER, "Jane", "jane@example.com", "hash", "514-555-0101")));

        mockMvc.perform(get("/api/auth/session").header("Authorization", "Bearer abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc123"))
                .andExpect(jsonPath("$.name").value("Jane"));
    }

    @Test
    void rejectsAnExpiredSession() throws Exception {
        when(sessionService.find("expired")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/auth/session").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesBearerToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer abc123"))
                .andExpect(status().isNoContent());
        verify(loginService).logout("abc123");
    }

    @Test
    void logoutRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Basic abc123"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(loginService);
    }

}
