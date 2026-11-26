package com.council.tenderapp.ord.services;

import java.util.Date;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.dlv.services.AssignmentService;
import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.dtos.OrderResponse;
import com.council.tenderapp.ord.dtos.OrderLineDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderLineRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.prc.service.LedgerService;
import com.council.tenderapp.technicalservices.payment.TenderCash;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final List<OrderStatus> PREPARATION_STATUSES =
        List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING);

    private final OrderRepository orderRepository;
    private final OrderStatusPublisher orderStatusPublisher;
    private final OrderEventRepository orderEventRepository;
    private final OrderLineRepository orderLineRepository;
    private final TenderCash tenderCash;
    private final LedgerService ledgerService;
    private final AssignmentService assignments;
    private final DispatchService dispatch;
    private final OrderResponseAssembler responses;

    public OrderService(
        OrderRepository orderRepository,
        OrderStatusPublisher orderStatusPublisher,
        OrderEventRepository orderEventRepository,
        ProfileRepository profileRepository,
        OrderLineRepository orderLineRepository,
        TenderCash tenderCash,
        CourierProfileRepository courierProfileRepository,
        LedgerService ledgerService,
        AssignmentService assignments,
        DispatchService dispatch, AssignmentRepository assignmentHistory
    ) {
        this.orderRepository = orderRepository;
        this.orderStatusPublisher = orderStatusPublisher;
        this.orderEventRepository = orderEventRepository;
        this.orderLineRepository = orderLineRepository;
        this.tenderCash = tenderCash;
        this.ledgerService = ledgerService;
        this.assignments = assignments;
        this.dispatch = dispatch;
        this.responses = new OrderResponseAssembler(profileRepository, courierProfileRepository,
            orderEventRepository, assignmentHistory);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(Long restaurantId) {
        // Read the database on every refresh so terminal statuses remain visible.
        log.info("Loading restaurant orders: restaurantId={}", restaurantId);
        return responses.toResponses(orderRepository.findByRestaurantIdOrderByDateOrderedAscIdAsc(restaurantId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPreparationQueue(Long restaurantId) {
        return responses.toResponses(orderRepository.findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
            restaurantId, PREPARATION_STATUSES
        ));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getIncomingOrders(Long restaurantId) {
        return responses.toResponses(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId, OrderStatus.PLACED
        ));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPickupQueue(Long restaurantId) {
        return responses.toResponses(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
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
        return responses.toResponse(savedOrder);
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
        return responses.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long restaurantId, Long orderId) {
        assignments.lockCouriers();
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.CANCELLED, "cancel");
        tenderCash.refunded(order);
        Order savedOrder = orderRepository.save(order);
        assignments.finish(savedOrder, false);
        orderRepository.flush();
        dispatch.retryQueue();
        publishStatusChange(savedOrder, previousStatus, "cancelled", null);
        return responses.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse startPreparation(Long restaurantId, Long orderId) {
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == OrderStatus.ACCEPTED) requireOldestAccepted(restaurantId, orderId);
        transitionOrder(order, OrderStatus.PREPARING, "prepare");
        Order savedOrder = orderRepository.save(order);
        publishStatusChange(savedOrder, previousStatus, "started preparing", null);
        return responses.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markReady(Long restaurantId, Long orderId) {
        assignments.lockCouriers();
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        transitionOrder(order, OrderStatus.READY, "mark ready");
        Order savedOrder = orderRepository.save(order);
        orderEventRepository.saveAndFlush(new OrderEvent(orderId, previousStatus.name().toLowerCase(java.util.Locale.ROOT),
                "ready", "order_ready", Instant.now()));
        orderRepository.flush();
        dispatch.retryQueue();
        publishStatusChange(savedOrder, previousStatus, "marked ready", null);
        return responses.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markPickedUpByCourier(Long courierId, Long orderId) {
        assignments.lockCouriers();
        Order order = getAssignedOrderForUpdate(courierId, orderId);

        OrderStatus previousStatus = order.getStatus();
        assignments.pickUp(order);
        transitionOrder(order, OrderStatus.PICKED_UP, "mark picked up");

        Instant now=Instant.now();
        order.setPickupDate(Date.from(now));
        Order savedOrder = orderRepository.save(order);

        orderEventRepository.save(new OrderEvent(
        savedOrder.getId(),
        previousStatus.name().toLowerCase(Locale.ROOT),
        OrderStatus.PICKED_UP.name().toLowerCase(Locale.ROOT),
        "order_picked_up",
        now));

        publishStatusChange(savedOrder, previousStatus, "marked picked up by courier", null);
        return responses.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markDeliveredByCourier(Long courierId, Long orderId) {
        assignments.lockCouriers();
        Order order = getAssignedOrderForUpdate(courierId, orderId);

        OrderStatus previousStatus = order.getStatus();
        assignments.current(order);
        transitionOrder(order, OrderStatus.DELIVERED, "mark delivered");

        Instant now = Instant.now();
        order.setDateDelivered(Date.from(now));
        Order savedOrder = orderRepository.save(order);
        assignments.finish(savedOrder, true);
        orderRepository.flush();
        dispatch.retryQueue();

        orderEventRepository.save(new OrderEvent(
        savedOrder.getId(),
        previousStatus.name().toLowerCase(Locale.ROOT),
        OrderStatus.DELIVERED.name().toLowerCase(Locale.ROOT),
        "order_delivered",
        now));

        publishStatusChange(savedOrder, previousStatus, "marked delivered", null);
        return responses.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse confirmDelivery(Long courierId, Long orderId) {
        return markDeliveredByCourier(courierId, orderId);
    }

    @Transactional
    public OrderResponse refundOrder(Long restaurantId, Long orderId) {
        assignments.lockCouriers();
        Order order = getOwnedOrderForUpdate(restaurantId, orderId);
        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != OrderStatus.DELIVERED) {
            throw invalidTransition(previousStatus, "refund");
        }
        assignments.current(order);
        transitionOrder(order, OrderStatus.REFUNDED, "refund");
        tenderCash.refunded(order);
        Order savedOrder = orderRepository.save(order);
        // Close any assignment still open on the order and free its courier, then let them take a waiting order
        assignments.finish(savedOrder, false);
        orderRepository.flush();
        dispatch.retryQueue();
        ledgerService.createRefundEntry(savedOrder);
        orderEventRepository.saveAndFlush(new OrderEvent(
            orderId,
            previousStatus.name().toLowerCase(java.util.Locale.ROOT),
            OrderStatus.REFUNDED.name().toLowerCase(java.util.Locale.ROOT),
            "order_refunded",
            Instant.now()
        ));
        publishStatusChange(savedOrder, previousStatus, "refunded", null);
        return responses.toResponse(savedOrder);
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
        if (!OrderAccessPolicy.isFromRestaurant(order, restaurantId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Order does not belong to the restaurant"
            );
        }
        return order;
    }

    private Order getAssignedOrderForUpdate(Long courierId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!OrderAccessPolicy.isAssignedTo(order, courierId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order is not assigned to this courier");
        }
        return order;
    }

    // Kitchens prepare accepted orders first-come, first-served by the time they were placed
    private void requireOldestAccepted(Long restaurantId, Long orderId) {
        List<Order> accepted = orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId, OrderStatus.ACCEPTED
        );
        if (!accepted.isEmpty() && !Objects.equals(accepted.get(0).getId(), orderId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Start preparing order #" + accepted.get(0).getId() + " first; orders are prepared in the order they were placed"
            );
        }
    }

    private ResponseStatusException invalidTransition(OrderStatus currentStatus, String action) {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid order transition for action '" + action + "' from status " + currentStatus
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
