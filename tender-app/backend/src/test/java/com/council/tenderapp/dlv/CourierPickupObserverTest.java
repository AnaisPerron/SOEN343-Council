package com.council.tenderapp.dlv;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.dlv.observers.CourierPickupObserver;
import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;

@ExtendWith(MockitoExtension.class)
class CourierPickupObserverTest {
    @Mock private CourierDutyService courierDutyService;
    @InjectMocks private CourierPickupObserver observer;

    private static OrderStatusEvent change(OrderStatus from, OrderStatus to) {
        return new OrderStatusEvent("EVENT01", 12L, 1L, from, to, "test", Instant.now(), null);
    }

    @Test
    void theCourierCarriesTheOrderOncePickedUp() {
        observer.onOrderStatusChanged(change(OrderStatus.READY, OrderStatus.PICKED_UP));
        verify(courierDutyService).startCarrying(12L);
    }

    @Test
    void ignoresEveryOtherChange() {
        observer.onOrderStatusChanged(change(OrderStatus.PREPARING, OrderStatus.READY));
        observer.onOrderStatusChanged(change(OrderStatus.PICKED_UP, OrderStatus.DELIVERED));
        verify(courierDutyService, never()).startCarrying(any());
    }
}
