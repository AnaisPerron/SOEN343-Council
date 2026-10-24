package com.council.tenderapp.ord.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.technicalservices.logging.AppLogService;

// A restaurant's queue is its orders in FIFO order of id (orders has no timestamp column); enqueuing records the order_placed event
@Service
public class PreparationQueueService {

    private final OrderEventRepository orderEventRepository;
    private final AppLogService logService;

    public PreparationQueueService(OrderEventRepository orderEventRepository, AppLogService logService) {
        this.orderEventRepository = orderEventRepository;
        this.logService = logService;
    }

    public void enqueue(Order order, Instant placedAt) {
        orderEventRepository.save(new OrderEvent(order.getId(), null, Order.STATUS_PLACED, "order_placed", placedAt));
        logService.info("Order " + order.getId() + " added to restaurant " + order.getRestaurantId() + " preparation queue");
    }

    // The order has already left the queue by changing status; this records the order_cancelled event
    public void remove(Order order, String previousStatus, Instant removedAt) {
        orderEventRepository.save(new OrderEvent(order.getId(), previousStatus, order.getStatus(), "order_cancelled", removedAt));
        logService.info("Order " + order.getId() + " removed from restaurant " + order.getRestaurantId() + " preparation queue");
    }
}
