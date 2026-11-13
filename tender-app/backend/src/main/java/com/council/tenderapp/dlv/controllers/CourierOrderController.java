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
import com.council.tenderapp.dlv.dtos.CourierDutyResponseDto;
import com.council.tenderapp.dlv.dtos.DeliveryConfirmationDto;
import com.council.tenderapp.dlv.dtos.PickupConfirmationDto;
import com.council.tenderapp.dlv.services.CourierDeliveryService;
import com.council.tenderapp.dlv.services.CourierDutyService;



@RestController
@RequestMapping("/api/courier/orders")
@PreAuthorize("hasRole('COURIER')")
public class CourierOrderController {

    private final CourierDeliveryService courierDeliveryService;
    private final CourierDutyService courierDutyService;

    public CourierOrderController(CourierDeliveryService courierDeliveryService,CourierDutyService courierDutyService) {
        this.courierDeliveryService = courierDeliveryService;
        this.courierDutyService=courierDutyService;
    }

    private Long getCourierId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }

    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<PickupConfirmationDto> confirmPickup(
            Authentication authentication, @PathVariable Long orderId) {
        Long courierId = getCourierId(authentication);
        AssignedOrderDto order = courierDeliveryService.confirmPickup(courierId, orderId);
        CourierDutyResponseDto duty = courierDutyService.getDutyStatus(courierId);

        return ResponseEntity.ok(new PickupConfirmationDto(order.orderId(), order.status(), duty.status()));
    }

    @PostMapping("/{orderId}/delivery")
    public ResponseEntity<DeliveryConfirmationDto> confirmDelivery(
            Authentication authentication, @PathVariable Long orderId) {
        Long courierId = getCourierId(authentication);
        AssignedOrderDto order = courierDeliveryService.confirmDelivery(courierId, orderId);
        CourierDutyResponseDto duty = courierDutyService.getDutyStatus(courierId);

        return ResponseEntity.ok(new DeliveryConfirmationDto(order.orderId(), order.status(), duty.status()));
    }
}
