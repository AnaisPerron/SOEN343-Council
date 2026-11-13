package com.council.tenderapp.dlv.controllers;

import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.dlv.services.CourierOfferService;

@RestController
@RequestMapping("/api/courier/offers")
@PreAuthorize("hasRole('COURIER')")
public class CourierOfferController {
    private final CourierOfferService service;

    public CourierOfferController(CourierOfferService service) { this.service = service; }

    @PostMapping("/{assignmentId}/accept")
    public AssignedOrderDto accept(Authentication authentication, @PathVariable @Positive long assignmentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return service.accept(id, assignmentId);
    }
}
