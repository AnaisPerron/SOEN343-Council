package com.council.tenderapp.dlv;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.dlv.observers.DispatchOnReadyObserver;
import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;

@ExtendWith(MockitoExtension.class)
class DispatchOnReadyObserverTest {
    @Mock private DispatchService dispatchService;
    @InjectMocks private DispatchOnReadyObserver observer;

    private static OrderStatusEvent change(OrderStatus from, OrderStatus to) {
        return new OrderStatusEvent("EVENT01", 12L, 1L, from, to, "test", Instant.now(), null);
    }

    @Test
    void dispatchesWhenAnOrderBecomesReady() {
        observer.onOrderStatusChanged(change(OrderStatus.PREPARING, OrderStatus.READY));
        verify(dispatchService).retryQueueAfterOrderReady();
    }

    @Test
    void ignoresOtherStatusChanges() {
        observer.onOrderStatusChanged(change(OrderStatus.ACCEPTED, OrderStatus.PREPARING));
        observer.onOrderStatusChanged(change(OrderStatus.READY, OrderStatus.PICKED_UP));
        observer.onOrderStatusChanged(change(OrderStatus.PICKED_UP, OrderStatus.DELIVERED));
        verify(dispatchService, never()).retryQueueAfterOrderReady();
    }
}
