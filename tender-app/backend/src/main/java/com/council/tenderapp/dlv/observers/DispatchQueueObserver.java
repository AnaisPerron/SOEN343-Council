package com.council.tenderapp.dlv.observers;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.dlv.services.AssignmentService;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.ord.observers.OrderObserver;
import com.council.tenderapp.ord.repositories.OrderEventRepository;

@Component
public class DispatchQueueObserver implements OrderObserver {

    private final OrderEventRepository orderEventRepository;
    private final DispatchService dispatchService;
    private final AssignmentService assignments;

    public DispatchQueueObserver(OrderEventRepository orderEventRepository, DispatchService dispatchService, AssignmentService assignments) {
        this.orderEventRepository = orderEventRepository;
        this.dispatchService = dispatchService;
        this.assignments = assignments;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderStatusChanged(OrderStatusEvent event) {
        if (event.newStatus() != OrderStatus.READY || event.previousStatus() == OrderStatus.READY) {
            return;
        }

        assignments.lockCouriers();
        if (!orderEventRepository.existsByOrderIdAndEventType(event.orderId(), "order_ready")) {
            orderEventRepository.saveAndFlush(new OrderEvent(
                    event.orderId(),
                    event.previousStatus() == null ? null : event.previousStatus().name().toLowerCase(Locale.ROOT),
                    event.newStatus().name().toLowerCase(Locale.ROOT),
                    "order_ready",
                    event.occurredAt()));
        }
        dispatchService.retryQueue();
    }
}
