package com.council.tenderapp.operator.controllers;

import org.springframework.context.annotation.Profile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.council.tenderapp.operator.dto.ResetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.operator.services.DemoSeedResetService;

/** The reset route is not registered outside the explicitly enabled demo profile. */
@RestController
@RequestMapping("/api/operator")
@Profile("demo")
public class DemoSeedResetController {
    private final DemoSeedResetService service;

    public DemoSeedResetController(DemoSeedResetService service) {
        this.service = service;
    }

    @PostMapping("/reset-seed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetSeed(@Valid @RequestBody ResetRequest request) {
        service.reset();
    }
}
