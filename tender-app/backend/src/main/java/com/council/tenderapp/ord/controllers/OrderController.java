package com.council.tenderapp.ord.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.dtos.OrderResponseDto;
import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.services.CustomerOrderService;
import com.council.tenderapp.ord.dto.response.OrderResponse;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CustomerOrderService orderService;

    public OrderController(CustomerOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponse> history(@AuthenticationPrincipal Long customerId) {
        return orderService.getHistory(customerId);
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto placeOrder(@AuthenticationPrincipal Long customerId,
            @Valid @RequestBody PlaceOrderRequestDto request) {
        return orderService.placeOrder(customerId, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderStatusResponseDto cancelOrder(@AuthenticationPrincipal Long customerId, @PathVariable Long id) {
        return orderService.cancelOrder(customerId, id);
    }
}
