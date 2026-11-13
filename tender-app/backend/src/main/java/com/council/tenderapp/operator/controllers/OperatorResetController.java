package com.council.tenderapp.operator.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.operator.dto.ResetRequest;
import com.council.tenderapp.operator.services.OperatorResetService;

@RestController
@RequestMapping("/api/operator/reset")
public class OperatorResetController {
    private final OperatorResetService service;

    public OperatorResetController(OperatorResetService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetRequest request) { service.reset(); }
}
