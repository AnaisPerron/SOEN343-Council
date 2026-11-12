package com.council.tenderapp.dlv.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.dlv.services.CourierDeliveryService;


@RestController
@RequestMapping("/api/courier/orders")
@PreAuthorize("hasRole('COURIER')")
public class CourierOrderController {

    private final CourierDeliveryService courierDeliveryService;

    public CourierOrderController(CourierDeliveryService courierDeliveryService) {
        this.courierDeliveryService = courierDeliveryService;
    }

    private Long getCourierId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }

    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<AssignedOrderDto> confirmPickup(
            Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(courierDeliveryService.confirmPickup(getCourierId(authentication), orderId));
    }
}