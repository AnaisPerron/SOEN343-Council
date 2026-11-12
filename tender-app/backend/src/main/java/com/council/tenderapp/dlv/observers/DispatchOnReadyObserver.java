package com.council.tenderapp.dlv.observers;

import org.springframework.stereotype.Component;

import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.ord.observers.OrderObserver;

/** Offers an order to an idle courier as soon as the restaurant marks it ready (R-DLV-04). */
@Component
public class DispatchOnReadyObserver implements OrderObserver {

    private final DispatchService dispatchService;

    public DispatchOnReadyObserver(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Override
    public void onOrderStatusChanged(OrderStatusEvent event) {
        // Couriers who were already idle are only matched when an order becomes ready
        if (event.newStatus() == OrderStatus.READY && event.previousStatus() != OrderStatus.READY) {
            dispatchService.retryQueueAfterOrderReady();
        }
    }
}
