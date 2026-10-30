package com.council.tenderapp.ord.controllers;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.council.tenderapp.ord.commands.OrderCommandInvoker;
import com.council.tenderapp.ord.commands.OrderCommands;
import com.council.tenderapp.ord.dto.request.RejectOrderRequest;
import com.council.tenderapp.ord.dto.response.OrderResponse;
import com.council.tenderapp.ord.observers.DashboardObserver;
import com.council.tenderapp.ord.services.OrderService;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:8081}")
@RequestMapping("/api/restaurants/{restaurantId}/orders")
@PreAuthorize("hasRole('STAFF') and authentication.details.restaurantId == #restaurantId")
public class RestaurantOrderController {
    private final OrderService orderService;
    private final OrderCommandInvoker commandInvoker;
    private final DashboardObserver dashboardObserver;

    public RestaurantOrderController(
        OrderService orderService,
        OrderCommandInvoker commandInvoker,
        DashboardObserver dashboardObserver
    ) {
        this.orderService = orderService;
        this.commandInvoker = commandInvoker;
        this.dashboardObserver = dashboardObserver;
    }

    // Expose all statuses to the dashboard, including completed and rejected orders.
    @GetMapping
    public List<OrderResponse> getAllOrders(@PathVariable Long restaurantId) {
        return orderService.getAllOrders(restaurantId);
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

    @GetMapping(path = "/events", produces = "text/event-stream")
    public SseEmitter observeOrderStatus(@PathVariable Long restaurantId) {
        return dashboardObserver.subscribe(restaurantId);
    }

    @PostMapping("/{orderId}/accept")
    public OrderResponse acceptOrder(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return commandInvoker.execute(new OrderCommands.Accept(restaurantId, orderId));
    }

    @PostMapping("/{orderId}/reject")
    public OrderResponse rejectOrder(
        @PathVariable Long restaurantId,
        @PathVariable Long orderId,
        @Valid @RequestBody RejectOrderRequest request
    ) {
        return commandInvoker.execute(new OrderCommands.Reject(restaurantId, orderId, request.reason()));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return commandInvoker.execute(new OrderCommands.Cancel(restaurantId, orderId));
    }

    @PostMapping("/{orderId}/prepare")
    public OrderResponse startPreparation(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return commandInvoker.execute(new OrderCommands.StartPreparation(restaurantId, orderId));
    }

    @PostMapping("/{orderId}/ready")
    public OrderResponse markReady(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return commandInvoker.execute(new OrderCommands.MarkReady(restaurantId, orderId));
    }

    @PostMapping("/{orderId}/picked-up")
    public OrderResponse markPickedUp(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return commandInvoker.execute(new OrderCommands.MarkPickedUp(restaurantId, orderId));
    }

    @PostMapping("/{orderId}/delivered")
    public OrderResponse markDelivered(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return commandInvoker.execute(new OrderCommands.MarkDelivered(restaurantId, orderId));
    }

    @PostMapping("/{orderId}/refund")
    public OrderResponse refundOrder(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return commandInvoker.execute(new OrderCommands.Refund(restaurantId, orderId));
    }
}
