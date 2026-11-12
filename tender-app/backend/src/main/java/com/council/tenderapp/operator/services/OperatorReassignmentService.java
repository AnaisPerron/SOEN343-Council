package com.council.tenderapp.operator.services;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.dlv.services.AssignmentService;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.services.OrderStatusPublisher;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@Service
@PreAuthorize("hasRole('OPERATOR')")
public class OperatorReassignmentService {
    private static final Logger log = LoggerFactory.getLogger(OperatorReassignmentService.class);
    private final OrderRepository orders;
    private final CourierProfileRepository couriers;
    private final OrderEventRepository events;
    private final DispatchService dispatch;
    private final OrderStatusPublisher publisher;
    private final AssignmentService assignments;

    public OperatorReassignmentService(OrderRepository orders, CourierProfileRepository couriers,
            OrderEventRepository events, DispatchService dispatch, OrderStatusPublisher publisher, AssignmentService assignments) {
        this.orders = orders;
        this.couriers = couriers;
        this.events = events;
        this.dispatch = dispatch;
        this.publisher = publisher;
        this.assignments = assignments;
    }

    @Transactional
    public void reassign(long orderId, long expectedCourierId) {
        assignments.lockCouriers();
        var courier = couriers.findWithLockByUserId(expectedCourierId);
        var order = orders.findWithLockById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        if (order.getStatus() != OrderStatus.READY || !Long.valueOf(expectedCourierId).equals(order.getCourierId())) {
            throw new InvalidOrderStateException("Only a ready order assigned to the expected courier can be reassigned");
        }
        if (courier.isEmpty() || (courier.get().getStatus() != CourierStatus.ASSIGNED
                && courier.get().getStatus() != CourierStatus.OFFLINE)) {
            throw new InvalidOrderStateException("The previous courier must be assigned or offline");
        }
        if (orders.existsByCourierIdAndIdNotAndStatusIn(expectedCourierId, orderId,
                List.of(OrderStatus.READY, OrderStatus.PICKED_UP))) {
            throw new InvalidOrderStateException("The courier has another active order that must be resolved first");
        }
        assignments.end(order);
        courier.get().setStatus(CourierStatus.OFFLINE);
        order.setCourierId(null);
        events.saveAndFlush(new OrderEvent(orderId, "ready", "ready", "courier_reassigned", Instant.now()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Operator requeued order {} from courier {}", orderId, expectedCourierId);
                try {
                    dispatch.retryQueueAfterCommit();
                } catch (RuntimeException exception) {
                    log.error("Dispatch retry failed after reassignment of order {} the order remains queued", orderId, exception);
                }
                publisher.publish(order, OrderStatus.READY, "courier_reassigned", null);
            }
        });
    }
}
