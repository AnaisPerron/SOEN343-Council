package com.council.tenderapp.ORD.services;

import com.council.tenderapp.ORD.entities.Order;
import com.council.tenderapp.ORD.entities.OrderStatus;
import com.council.tenderapp.ORD.repositories.OrderRepository;
import com.council.tenderapp.ORD.dto.response.OrderLineResponse;
import com.council.tenderapp.ORD.dto.response.OrderResponse;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final List<OrderStatus> PREPARATION_STATUSES =
        List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING);

    private final OrderRepository orderRepository;
    private long eventSequence;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPreparationQueue(Long restaurantId) {
        return orderRepository
            .findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(restaurantId, PREPARATION_STATUSES)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getIncomingOrders(Long restaurantId) {
        return orderRepository
            .findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(restaurantId, OrderStatus.PLACED)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPickupQueue(Long restaurantId) {
        return orderRepository
            .findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(restaurantId, OrderStatus.READY)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public OrderResponse acceptOrder(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.ACCEPTED, "accept");
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "accepted", savedOrder.getStatus(), null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse rejectOrder(Long restaurantId, Long orderId, String reason) {
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }

        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.REJECTED, "reject");
        order.setRejectionReason(trimmedReason);
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "rejected", savedOrder.getStatus(), trimmedReason);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.CANCELLED, "cancel");
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "cancelled", savedOrder.getStatus(), null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse startPreparation(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.PREPARING, "prepare");
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "started preparing", savedOrder.getStatus(), null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markReady(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.READY, "mark ready");
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "marked ready", savedOrder.getStatus(), null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markPickedUp(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.PICKED_UP, "mark picked up");
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "marked picked up", savedOrder.getStatus(), null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markDelivered(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.DELIVERED, "mark delivered");
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "marked delivered", savedOrder.getStatus(), null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse refundOrder(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        transitionOrder(order, OrderStatus.REFUNDED, "refund");
        Order savedOrder = orderRepository.save(order);
        logStatusChange(restaurantId, orderId, "refunded", savedOrder.getStatus(), null);
        return toResponse(savedOrder);
    }

    private void transitionOrder(Order order, OrderStatus nextStatus, String action) {
        OrderStatus currentStatus = order.getStatus();
        try {
            order.transitionTo(nextStatus);
        } catch (IllegalStateException exception) {
            throw invalidTransition(currentStatus, action);
        }
    }

    private synchronized void logStatusChange(
        Long restaurantId,
        Long orderId,
        String action,
        OrderStatus newStatus,
        String rejectionReason
    ) {
        String eventId = String.format(Locale.ROOT, "EVENT%02d", ++eventSequence);
        String reasonDetails = rejectionReason == null
            ? ""
            : " | Rejection reason: \"" + rejectionReason
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ") + "\"";
        logger.info(
            "{}: Restaurant_{} {} order #{}: new status: {}{}",
            eventId,
            restaurantId,
            action,
            orderId,
            newStatus,
            reasonDetails
        );
    }

    private Order getOwnedOrderForUpdate(Long restaurantId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!Objects.equals(order.getRestaurantId(), restaurantId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Order does not belong to the restaurant"
            );
        }
        return order;
    }

    private ResponseStatusException invalidTransition(OrderStatus currentStatus, String action) {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid order transition for action '" + action + "' from status " + currentStatus
        );
    }

    private OrderResponse toResponse(Order order) {
        List<OrderLineResponse> orderLines = order.getOrderLines().stream()
            .map(line ->
                new OrderLineResponse(line.getMenuItem().getName(), line.getQuantity(), line.getUnitPrice())
            )
            .toList();
        return new OrderResponse(
            order.getId(),
            order.getStatus(),
            order.getDateOrdered(),
            order.getAcceptedAt(),
            order.getRejectionReason(),
            order.getTotalPrice(),
            orderLines
        );
    }
}
