package com.council.tenderapp.ord.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.dtos.OrderResponseDto;
import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.services.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // The token filter sets the account id as the principal
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto placeOrder(@AuthenticationPrincipal Long customerId, @Valid @RequestBody PlaceOrderRequestDto request) {
        return orderService.placeOrder(customerId, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderStatusResponseDto cancelOrder(@AuthenticationPrincipal Long customerId, @PathVariable Long id) {
        return orderService.cancelOrder(customerId, id);
    }
}
