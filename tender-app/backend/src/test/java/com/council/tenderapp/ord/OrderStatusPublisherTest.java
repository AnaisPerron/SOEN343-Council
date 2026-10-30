package com.council.tenderapp.ord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.List;
import org.mockito.ArgumentCaptor;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.events.OrderStatusEvent;
import com.council.tenderapp.ord.observers.OrderObserver;
import com.council.tenderapp.ord.services.OrderStatusPublisher;

import org.junit.jupiter.api.Test;

class OrderStatusPublisherTest {

    @Test
    void notifiesEveryObserverWithSequentialStatusEvents() {
        OrderObserver firstObserver = mock(OrderObserver.class);
        OrderObserver secondObserver = mock(OrderObserver.class);
        OrderStatusPublisher publisher = new OrderStatusPublisher(List.of(firstObserver, secondObserver));
        Order order = new Order(42L, new Date(), OrderStatus.ACCEPTED);
        order.setId(17L);

        publisher.publish(order, OrderStatus.PLACED, "accepted", null);
        order.transitionTo(OrderStatus.PREPARING);
        publisher.publish(order, OrderStatus.ACCEPTED, "started preparing", null);

        ArgumentCaptor<OrderStatusEvent> firstEvent = ArgumentCaptor.forClass(OrderStatusEvent.class);
        verify(firstObserver, times(2)).onOrderStatusChanged(firstEvent.capture());
        verify(secondObserver, times(2)).onOrderStatusChanged(any(OrderStatusEvent.class));
        assertThat(firstEvent.getAllValues())
            .extracting(OrderStatusEvent::eventId)
            .containsExactly("EVENT01", "EVENT02");
        assertThat(firstEvent.getAllValues().get(0).previousStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(firstEvent.getAllValues().get(0).newStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(firstEvent.getAllValues().get(0).orderId()).isEqualTo(17L);
        assertThat(firstEvent.getAllValues().get(0).restaurantId()).isEqualTo(42L);
        assertThat(firstEvent.getAllValues().get(1).previousStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(firstEvent.getAllValues().get(1).newStatus()).isEqualTo(OrderStatus.PREPARING);
    }
}
