package com.council.tenderapp.ord.services;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.ord.observers.OrderObserver;

@Component
public class OrderStatusPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderStatusPublisher.class);

    private final List<OrderObserver> observers;
    private final AtomicLong eventSequence = new AtomicLong();

    public OrderStatusPublisher(List<OrderObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    public void publish(
        Order order,
        OrderStatus previousStatus,
        String action,
        String rejectionReason
    ) {
        String eventId = String.format(Locale.ROOT, "EVENT%02d", eventSequence.incrementAndGet());
        OrderStatusEvent event = new OrderStatusEvent(
            eventId,
            order.getId(),
            order.getRestaurantId(),
            previousStatus,
            order.getStatus(),
            action,
            Instant.now(),
            rejectionReason
        );

        for (OrderObserver observer : observers) {
            try {
                observer.onOrderStatusChanged(event);
            } catch (RuntimeException exception) {
                logger.error(
                    "Order observer {} failed while handling {}",
                    observer.getClass().getSimpleName(),
                    eventId,
                    exception
                );
            }
        }
    }
}
