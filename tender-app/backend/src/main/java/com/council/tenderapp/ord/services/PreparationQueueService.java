package com.council.tenderapp.ord.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.technicalservices.logging.AppLogService;

// A restaurant's queue is its orders in FIFO order of created_at; enqueuing records the order_placed event
@Service
public class PreparationQueueService {

    private final OrderEventRepository orderEventRepository;
    private final AppLogService logService;

    public PreparationQueueService(OrderEventRepository orderEventRepository, AppLogService logService) {
        this.orderEventRepository = orderEventRepository;
        this.logService = logService;
    }

    public void enqueue(Order order) {
        orderEventRepository.save(new OrderEvent(order.getId(), null, Order.STATUS_PLACED, "order_placed", Instant.now()));
        logService.info("Order " + order.getId() + " added to restaurant " + order.getRestaurantId() + " preparation queue");
    }
}
