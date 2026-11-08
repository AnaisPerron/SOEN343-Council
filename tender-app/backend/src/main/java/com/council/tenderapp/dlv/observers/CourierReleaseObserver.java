package com.council.tenderapp.dlv.observers;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.ord.observers.OrderObserver;

/** Sends the courier back to idle when the order they were holding is delivered or refunded. */
@Component
public class CourierReleaseObserver implements OrderObserver {

    // States in which a courier is holding the order
    private static final Set<OrderStatus> WITH_COURIER = Set.of(OrderStatus.READY, OrderStatus.PICKED_UP);
    private static final Set<OrderStatus> FINISHED = Set.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED);

    private final CourierDutyService courierDutyService;

    public CourierReleaseObserver(CourierDutyService courierDutyService) {
        this.courierDutyService = courierDutyService;
    }

    @Override
    public void onOrderStatusChanged(OrderStatusEvent event) {
        // A refund after delivery must not touch the courier, who may already be on another order
        if (FINISHED.contains(event.newStatus()) && WITH_COURIER.contains(event.previousStatus())) {
            courierDutyService.releaseCourier(event.orderId());
        }
    }
}
