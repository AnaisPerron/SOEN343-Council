package com.council.tenderapp.reg.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.exceptions.DuplicateEmailException;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.RegistrationService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

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

}
