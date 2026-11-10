package com.council.tenderapp.dlv.observers;

import org.springframework.stereotype.Component;

import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.ord.observers.OrderObserver;

/** Moves the courier from assigned to carrying when the order they were sent for is picked up. */
@Component
public class CourierPickupObserver implements OrderObserver {

    private final CourierDutyService courierDutyService;

    public CourierPickupObserver(CourierDutyService courierDutyService) {
        this.courierDutyService = courierDutyService;
    }

    @Override
    public void onOrderStatusChanged(OrderStatusEvent event) {
        if (event.previousStatus() == OrderStatus.READY && event.newStatus() == OrderStatus.PICKED_UP) {
            courierDutyService.startCarrying(event.orderId());
        }
    }
}
