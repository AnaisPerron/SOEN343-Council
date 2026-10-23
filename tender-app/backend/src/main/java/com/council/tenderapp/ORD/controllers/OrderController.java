package com.council.tenderapp.ORD.controllers;

import com.council.tenderapp.ORD.services.OrderService;
import com.council.tenderapp.ORD.persistence.OrderResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping({"/preparation", "/arrival-queue"})
    public List<OrderResponse> getPreparationQueue(@PathVariable Long restaurantId) {
        return orderService.getPreparationQueue(restaurantId);
    }
}
