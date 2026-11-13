package com.council.tenderapp.dlv.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.dtos.OrderResponse;
import com.council.tenderapp.ord.services.OrderService;

@RestController
@RequestMapping("/api/courier/orders")
@PreAuthorize("hasRole('COURIER')")
public class CourierDeliveryController {
    private final OrderService orders;

    public CourierDeliveryController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/{orderId}/delivered")
    public OrderResponse confirmDelivery(@AuthenticationPrincipal Long courierId, @PathVariable Long orderId) {
        return orders.confirmDelivery(courierId, orderId);
    }
}
