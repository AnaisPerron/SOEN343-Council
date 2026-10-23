package com.council.tenderapp.ORD.controllers;

import com.council.tenderapp.ORD.services.OrderService;
import com.council.tenderapp.ORD.dto.request.RejectOrderRequest;
import com.council.tenderapp.ORD.dto.response.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:8081}")
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

    @GetMapping("/incoming")
    public List<OrderResponse> getIncomingOrders(@PathVariable Long restaurantId) {
        return orderService.getIncomingOrders(restaurantId);
    }

    @GetMapping("/pickup")
    public List<OrderResponse> getPickupQueue(@PathVariable Long restaurantId) {
        return orderService.getPickupQueue(restaurantId);
    }

    @PostMapping("/{orderId}/accept")
    public OrderResponse acceptOrder(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return orderService.acceptOrder(restaurantId, orderId);
    }

    @PostMapping("/{orderId}/reject")
    public OrderResponse rejectOrder(
        @PathVariable Long restaurantId,
        @PathVariable Long orderId,
        @Valid @RequestBody RejectOrderRequest request
    ) {
        return orderService.rejectOrder(restaurantId, orderId, request.reason());
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return orderService.cancelOrder(restaurantId, orderId);
    }

    @PostMapping("/{orderId}/prepare")
    public OrderResponse startPreparation(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return orderService.startPreparation(restaurantId, orderId);
    }

    @PostMapping("/{orderId}/ready")
    public OrderResponse markReady(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return orderService.markReady(restaurantId, orderId);
    }

    @PostMapping("/{orderId}/picked-up")
    public OrderResponse markPickedUp(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return orderService.markPickedUp(restaurantId, orderId);
    }

    @PostMapping("/{orderId}/delivered")
    public OrderResponse markDelivered(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return orderService.markDelivered(restaurantId, orderId);
    }

    @PostMapping("/{orderId}/refund")
    public OrderResponse refundOrder(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return orderService.refundOrder(restaurantId, orderId);
    }
}
