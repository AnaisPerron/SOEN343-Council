package com.council.tenderapp.ord.services;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.technicalservices.payment.TenderCash;

@Service
@Transactional
public class OrderCancellationService {
    private final OrderRepository orders;
    private final PreparationQueueService queue;
    private final OrderStatusPublisher publisher;
    private final TenderCash tenderCash;

    public OrderCancellationService(OrderRepository orders, PreparationQueueService queue,
            OrderStatusPublisher publisher, TenderCash tenderCash) {
        this.orders = orders;
        this.queue = queue;
        this.publisher = publisher;
        this.tenderCash = tenderCash;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderStatusResponseDto cancelForCustomer(Long customerId, Long orderId) {
        Order order = lockedOrder(orderId);
        if (!OrderAccessPolicy.isPlacedBy(order, customerId)) {
            throw new OrderAccessDeniedException("Order " + orderId + " does not belong to this customer");
        }
        return cancel(order);
    }

    private Order lockedOrder(Long orderId) {
        return orders.findWithLockById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
    }

    private OrderStatusResponseDto cancel(Order order) {
        OrderStatus previous = order.getStatus();
        if (previous != OrderStatus.PLACED) {
            throw new InvalidOrderStateException("Order " + order.getId()
                    + " cannot be cancelled because it is " + previous.name().toLowerCase(Locale.ROOT));
        }
        Instant cancelledAt = Instant.now();
        order.transitionTo(OrderStatus.CANCELLED);
        tenderCash.refunded(order);
        orders.save(order);
        queue.remove(order, "placed", cancelledAt);
        Runnable notify = () -> publisher.publish(order, previous, "cancelled", null);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { notify.run(); }
            });
        } else {
            notify.run();
        }
        return new OrderStatusResponseDto(order.getId(), "placed", "cancelled", cancelledAt);
    }
}
