package com.council.tenderapp.operator.controllers;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.operator.dto.CourierSummary;
import com.council.tenderapp.operator.dto.CustomerSummary;
import com.council.tenderapp.operator.dto.DashboardPage;
import com.council.tenderapp.operator.dto.DashboardSummary;
import com.council.tenderapp.operator.dto.OrderSummary;
import com.council.tenderapp.operator.dto.RestaurantSummary;
import com.council.tenderapp.operator.services.OperatorDashboardService;
import com.council.tenderapp.ord.dto.response.OrderResponse;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.ord.repositories.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/operator")
public class OperatorDashboardController {
    private final OperatorDashboardService service;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OperatorDashboardController(OperatorDashboardService service, OrderService orderService,
            OrderRepository orderRepository) {
        this.service = service;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/orders/{orderId}/refund")
    public OrderResponse refund(@PathVariable Long orderId) {
        Long restaurantId = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"))
                .getRestaurantId();
        return orderService.refundOrder(restaurantId, orderId);
    }

    @GetMapping("/summary")
    public DashboardSummary summary() { return service.summary(); }

    @GetMapping("/restaurants")
    public DashboardPage<RestaurantSummary> restaurants(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.restaurants(page, size);
    }

    @GetMapping("/orders")
    public DashboardPage<OrderSummary> orders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.orders(page, size);
    }

    @GetMapping("/customers")
    public DashboardPage<CustomerSummary> customers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.customers(page, size);
    }

    @GetMapping("/couriers")
    public DashboardPage<CourierSummary> couriers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.couriers(page, size);
    }
}
