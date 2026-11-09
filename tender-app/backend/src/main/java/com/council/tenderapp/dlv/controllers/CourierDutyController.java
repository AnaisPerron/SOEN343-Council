package com.council.tenderapp.dlv.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.dlv.dtos.CourierDutyResponseDto;
import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.ord.services.OrderService;

/** A courier changes only their own duty status: the courier is always the logged-in account. */
@RestController
@RequestMapping("/api/courier/duty")
@PreAuthorize("hasRole('COURIER')")
public class CourierDutyController {

    private final CourierDutyService courierDutyService;
    private final OrderService orderService;

    public CourierDutyController(CourierDutyService courierDutyService, OrderService orderService) {
        this.courierDutyService = courierDutyService;
        this.orderService = orderService;
    }

    private Long getCourierId(Authentication authentication)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }

    @GetMapping
    public ResponseEntity<CourierDutyResponseDto> getDutyStatus(Authentication authentication)
    {
        return ResponseEntity.ok(courierDutyService.getDutyStatus(getCourierId(authentication)));
    }

    @PostMapping("/on")
    public ResponseEntity<CourierDutyResponseDto> goOnDuty(Authentication authentication)
    {
        return ResponseEntity.ok(courierDutyService.goOnDuty(getCourierId(authentication)));
    }

    @PostMapping("/off")
    public ResponseEntity<CourierDutyResponseDto> goOffDuty(Authentication authentication)
    {
        return ResponseEntity.ok(courierDutyService.goOffDuty(getCourierId(authentication)));
    }

    @PostMapping("/orders/{orderId}/delivered")
    public ResponseEntity<CourierDutyResponseDto> markOrderDelivered(
            Authentication authentication,
            @PathVariable Long orderId)
    {
        Long courierId = getCourierId(authentication);
        orderService.markDeliveredByCourier(courierId, orderId);
        return ResponseEntity.ok(courierDutyService.getDutyStatus(courierId));
    }
}
