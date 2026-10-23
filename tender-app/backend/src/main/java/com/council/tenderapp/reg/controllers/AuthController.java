package com.council.tenderapp.reg.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.LoginResponse;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.dtos.RegisterResponse;
import com.council.tenderapp.reg.exceptions.MissingSessionTokenException;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RegistrationService registrationService;
    private final LoginService loginService;

    public AuthController(RegistrationService registrationService, LoginService loginService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    // Logging out an unknown or expired token still succeeds, so retries are safe
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)
                || authorization.substring(BEARER_PREFIX.length()).isBlank()) {
            throw new MissingSessionTokenException();
        }
        loginService.logout(authorization.substring(BEARER_PREFIX.length()).trim());
    }
}
