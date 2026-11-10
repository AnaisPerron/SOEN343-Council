package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.technicalservices.logging.AppLogService;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {
    @Mock private OrderRepository orders;
    @Mock private RestaurantRepository restaurants;
    @Mock private CourierProfileRepository couriers;
    @Mock private AppLogService logService;

    private static CourierProfile idleCourier(long id, Double lat, Double lon) {
        CourierProfile courier = new CourierProfile(id);
        courier.setStatus(CourierStatus.IDLE);
        courier.setLatitude(lat);
        courier.setLongitude(lon);
        return courier;
    }

    private static Order readyOrder(long restaurantId) {
        return new Order(restaurantId, new Date(), OrderStatus.READY);
    }

    @Test
    void offersTheNearestIdleCourier() {
        CourierProfile far = idleCourier(1, 45.60, -73.70);
        CourierProfile near = idleCourier(2, 45.501, -73.571);
        Order order = readyOrder(10);
        when(couriers.findByStatusOrderByUserIdAsc(CourierStatus.IDLE)).thenReturn(List.of(far, near));
        when(orders.findWaitingReadyOrdersForDispatch()).thenReturn(List.of(order));
        when(restaurants.findById(10L)).thenReturn(Optional.of(new Restaurant("Cafe", "open", 45.50, -73.57)));

        new DispatchService(orders, restaurants, couriers, logService).retryQueue();

        assertThat(order.getCourierId()).isEqualTo(2L);
        assertThat(near.getStatus()).isEqualTo(CourierStatus.IDLE);
        assertThat(far.getStatus()).isEqualTo(CourierStatus.IDLE);
    }

    @Test
    void ordersKeepWaitingOnceCouriersRunOut() {
        CourierProfile only = idleCourier(1, null, null);
        Order oldest = readyOrder(10);
        Order newer = readyOrder(10);
        when(couriers.findByStatusOrderByUserIdAsc(CourierStatus.IDLE)).thenReturn(List.of(only));
        when(orders.findWaitingReadyOrdersForDispatch()).thenReturn(List.of(oldest, newer));
        when(restaurants.findById(10L)).thenReturn(Optional.empty());

        new DispatchService(orders, restaurants, couriers, logService).retryQueue();

        assertThat(oldest.getCourierId()).isEqualTo(1L);
        assertThat(only.getStatus()).isEqualTo(CourierStatus.IDLE);
        assertThat(newer.getCourierId()).isNull();
    }

    @Test
    void readyOrdersRemainInTheQueueWhenNoCourierIsIdle() {
        Order waiting = readyOrder(10);
        waiting.setId(17L);
        when(couriers.findByStatusOrderByUserIdAsc(CourierStatus.IDLE)).thenReturn(List.of());
        when(orders.findWaitingReadyOrdersForDispatch())
                .thenReturn(List.of(waiting));

        new DispatchService(orders, restaurants, couriers, logService).retryQueue();

        assertThat(waiting.getCourierId()).isNull();
        verify(logService).info("Dispatch queue: order #17 is waiting for a courier");
    }
}
