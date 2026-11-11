package com.council.tenderapp.ord.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.ord.dto.response.OrderLineResponse;
import com.council.tenderapp.ord.dto.response.OrderResponse;
import com.council.tenderapp.ord.dtos.OrderLineDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderLineRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.technicalservices.payment.TenderCash;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final List<OrderStatus> PREPARATION_STATUSES =
        List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING);

    private final OrderRepository orderRepository;
    private final OrderStatusPublisher orderStatusPublisher;
    private final ProfileRepository profileRepository;
    private final OrderLineRepository orderLineRepository;
    private final TenderCash tenderCash;

    public OrderService(
        OrderRepository orderRepository,
        OrderStatusPublisher orderStatusPublisher,
        ProfileRepository profileRepository,
        OrderLineRepository orderLineRepository, TenderCash tenderCash
    ) {
        this.orderRepository = orderRepository;
        this.orderStatusPublisher = orderStatusPublisher;
        this.profileRepository = profileRepository;
        this.orderLineRepository = orderLineRepository;
        this.tenderCash = tenderCash;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(Long restaurantId) {
        // Read the database on every refresh so terminal statuses remain visible.
        log.info("Loading restaurant orders: restaurantId={}", restaurantId);
        return toResponses(orderRepository.findByRestaurantIdOrderByDateOrderedAscIdAsc(restaurantId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPreparationQueue(Long restaurantId) {
        return toResponses(orderRepository.findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
            restaurantId, PREPARATION_STATUSES
        ));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getIncomingOrders(Long restaurantId) {
        return toResponses(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId, OrderStatus.PLACED
        ));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPickupQueue(Long restaurantId) {
        return toResponses(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId, OrderStatus.READY
        ));
    }

    @Transactional
    public OrderResponse acceptOrder(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.ACCEPTED, "accept");
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "accepted", null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse rejectOrder(Long restaurantId, Long orderId, String reason) {
        String trimmedReason = reason == null ? "" : reason.trim();
        // The reason is optional in the restaurant dashboard; store no reason for an empty input.

        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.REJECTED, "reject");
        tenderCash.refunded(order);
        order.setRejectionReason(trimmedReason.isEmpty() ? null : trimmedReason);
        Order savedOrder = orderRepository.save(order);
        log.info("Restaurant order rejected: orderId={}, restaurantId={}", orderId, restaurantId);
        publishStatusChange(savedOrder, previousStatus, "rejected", trimmedReason);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.CANCELLED, "cancel");
        tenderCash.refunded(order);
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "cancelled", null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse startPreparation(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.PREPARING, "prepare");
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "started preparing", null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markReady(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.READY, "mark ready");
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "marked ready", null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markPickedUp(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.PICKED_UP, "mark picked up");
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "marked picked up", null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markDelivered(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.DELIVERED, "mark delivered");
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "marked delivered", null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse confirmDelivery(Long courierId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!Objects.equals(order.getCourierId(), courierId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order is not assigned to this courier");
        }
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.DELIVERED, "confirm delivery");
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "confirmed delivery", null);
        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse refundOrder(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != OrderStatus.DELIVERED) {
            throw invalidTransition(previousStatus, "refund");
        }
        transitionOrder(order, OrderStatus.REFUNDED, "refund");
        tenderCash.refunded(order);
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "refunded", null);
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

    private void publishStatusChange(
        Order order,
        OrderStatus previousStatus,
        String action,
        String rejectionReason
    ) {
        Runnable publish = () ->
            orderStatusPublisher.publish(order, previousStatus, action, rejectionReason);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
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
        Profile customer = order.getCustomerId() == null
            ? null
            : profileRepository.findById(order.getCustomerId()).orElse(null);
        return toResponse(order, customer);
    }

    // Load all customers for a list in one query instead of one per order.
    private List<OrderResponse> toResponses(List<Order> orders) {
        List<Long> customerIds = orders.stream()
            .map(Order::getCustomerId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, Profile> customers = customerIds.isEmpty()
            ? Map.of()
            : profileRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Profile::getId, Function.identity()));
        return orders.stream()
            .map(order -> toResponse(
                order,
                order.getCustomerId() == null ? null : customers.get(order.getCustomerId())
            ))
            .toList();
    }

    private OrderResponse toResponse(Order order, Profile customer) {
        List<OrderLineResponse> orderLines = order.getOrderLines().stream()
            .map(line ->
                new OrderLineResponse(line.getMenuItem().getName(), line.getQuantity(), line.getUnitPrice())
            )
            .toList();
        return new OrderResponse(
            order.getId(),
            order.getRestaurantId(),
            order.getStatus(),
            order.getDateOrdered(),
            order.getAcceptedAt(),
            order.getRejectionReason(),
            order.getTotalPrice(),
            customer == null ? null : customer.getName(),
            customer == null ? null : customer.getPhone(),
            order.getDeliveryAddress(),
            orderLines
        );
    }

    public List<OrderLineDto> getOrderLinesForOrder(Long orderId) {
        return orderLineRepository.findByOrderId(orderId).stream()
            .map(line -> new OrderLineDto(
                line.getMenuItem().getId(),
                line.getMenuItem().getName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getUnitPrice().multiply(java.math.BigDecimal.valueOf(line.getQuantity()))
            ))
            .toList();
    }
}
