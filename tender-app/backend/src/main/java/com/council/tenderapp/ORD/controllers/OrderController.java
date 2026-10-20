package com.council.tenderapp.ORD.controllers;

import com.council.tenderapp.ORD.services.OrderService;
import com.council.tenderapp.ORD.persistence.OrderResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping ("/api/restaurants/{restaurantId}/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/arrival-queue")
    public List<OrderResponse> getArrivingOrders(@RequestParam Long restaurantId) {
        return orderService.getArrivingOrders(restaurantId);
    }
