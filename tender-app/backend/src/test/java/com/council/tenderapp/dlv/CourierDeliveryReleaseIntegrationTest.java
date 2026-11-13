package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.dlv.entities.AssignmentStatus;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.dlv.services.AssignmentService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.services.RegistrationService;

// Not @Transactional: the courier is released after the order change commits, so each step must really commit.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:courier-release;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class CourierDeliveryReleaseIntegrationTest {
    @Autowired private RegistrationService registrationService;
    @Autowired private CourierDutyService courierDutyService;
    @Autowired private OrderService orderService;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private OrderRepository orders;
    @Autowired private OrderEventRepository orderEvents;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AssignmentRepository assignments;

    private CourierStatus statusOf(Long courierId) {
        return couriers.findById(courierId).orElseThrow().getStatus();
    }

    @Test
    void markingAnOrderReadyDispatchesItToAnIdleCourier() {
        Long courierId = registrationService.register(new RegisterRequest(
                "Ready Tester", "ready-dispatch-courier@example.com", "secret123", "514-555-0181", Role.COURIER)).id();
        Long restaurantId = restaurants.saveAndFlush(new Restaurant("Ready Cafe", "open", 45.50, -73.57)).getId();
        Long orderId = orders.saveAndFlush(new Order(restaurantId, new Date(), OrderStatus.PREPARING)).getId();
        try {
            courierDutyService.goOnDuty(courierId);

            orderService.markReady(restaurantId, orderId);

            assertThat(orders.findById(orderId).orElseThrow().getCourierId()).isEqualTo(courierId);
            assertThat(statusOf(courierId)).isEqualTo(CourierStatus.ASSIGNED);
            assertThat(orderEvents.findAll()).anySatisfy(event -> {
                assertThat(event.getOrderId()).isEqualTo(orderId);
                assertThat(event.getEventType()).isEqualTo("order_ready");
                assertThat(event.getNewStatus()).isEqualTo("ready");
            });
        } finally {
            if (statusOf(courierId) == CourierStatus.ASSIGNED) courierDutyService.declineOffer(courierId, orderId);
            if (statusOf(courierId) == CourierStatus.IDLE) courierDutyService.goOffDuty(courierId);
            jdbc.update("DELETE FROM assignments WHERE order_id = ?", orderId);
            orders.deleteById(orderId);
        }
    }

    @Test
    void restaurantOrderResponseShowsCourierAfterOfferIsAccepted() {
        Long courierId = registrationService.register(new RegisterRequest(
                "Accepted Courier", "accepted-courier-name@example.com", "secret123", "514-555-0182", Role.COURIER)).id();
        Long restaurantId = restaurants.saveAndFlush(new Restaurant("Offer Cafe", "open", 45.50, -73.57)).getId();
        Long orderId = orders.saveAndFlush(new Order(restaurantId, new Date(), OrderStatus.READY)).getId();
        try {
            courierDutyService.goOnDuty(courierId);

            assertThat(orderService.getAllOrders(restaurantId))
                    .filteredOn(order -> order.getId().equals(orderId))
                    .singleElement()
                    .extracting(order -> order.getCourierName())
                    .isNull();

            courierDutyService.acceptOffer(courierId, orderId);
            assertThat(orderEvents.findAll()).anySatisfy(event -> {
                assertThat(event.getOrderId()).isEqualTo(orderId);
                assertThat(event.getEventType()).isEqualTo("courier_accepted");
            });

            assertThat(orderService.getAllOrders(restaurantId))
                    .filteredOn(order -> order.getId().equals(orderId))
                    .singleElement()
                    .extracting(order -> order.getCourierName())
                    .isEqualTo("Accepted Courier");
        } finally {
            if (statusOf(courierId) == CourierStatus.ASSIGNED) {
                orderService.markPickedUpByCourier(courierId, orderId);
                orderService.markDeliveredByCourier(courierId, orderId);
            }
            if (statusOf(courierId) == CourierStatus.IDLE) courierDutyService.goOffDuty(courierId);
            jdbc.update("DELETE FROM assignments WHERE order_id = ?", orderId);
            orders.deleteById(orderId);
        }
    }

    @Test
    void courierReturnsToIdleAfterDeliveringAndTakesTheNextWaitingOrder() {
        Long courierId = registrationService.register(new RegisterRequest(
                "Release Tester", "release-courier@example.com", "secret123", "514-555-0180", Role.COURIER)).id();
        Long restaurantId = restaurants.saveAndFlush(new Restaurant("Release Cafe", "open", 45.50, -73.57)).getId();
        long now = System.currentTimeMillis();
        Order firstReady = new Order(restaurantId, new Date(now), OrderStatus.READY);
        Long first = orders.saveAndFlush(firstReady).getId();
        Order secondReady = new Order(restaurantId, new Date(now - 60_000), OrderStatus.READY);
        Long second = orders.saveAndFlush(secondReady).getId();
        Instant readyTime = Instant.now();
        orderEvents.saveAndFlush(new OrderEvent(first, "preparing", "ready", "order_ready",
                readyTime.minusSeconds(60)));
        orderEvents.saveAndFlush(new OrderEvent(second, "preparing", "ready", "order_ready", readyTime));
        try {
            courierDutyService.goOnDuty(courierId);
            assertThat(orders.findById(first).orElseThrow().getCourierId()).isEqualTo(courierId);
            assertThat(orders.findById(second).orElseThrow().getCourierId()).isNull();

            courierDutyService.acceptOffer(courierId, first);
            orderService.markPickedUpByCourier(courierId, first);
            assertThat(statusOf(courierId)).isEqualTo(CourierStatus.CARRYING);

            orderService.markDeliveredByCourier(courierId, first);

            // Freed by the delivery, then immediately handed the order that was waiting
            assertThat(orders.findById(second).orElseThrow().getCourierId()).isEqualTo(courierId);
            assertThat(statusOf(courierId)).isEqualTo(CourierStatus.ASSIGNED);
            assertThat(orders.findById(first).orElseThrow().getCourierId()).isEqualTo(courierId);
            assertThat(orderService.getAllOrders(restaurantId))
                    .filteredOn(order -> order.getId().equals(first))
                    .singleElement()
                    .extracting(order -> order.getCourierName())
                    .isEqualTo("Release Tester");

            courierDutyService.acceptOffer(courierId, second);
            orderService.markPickedUpByCourier(courierId, second);
            orderService.markDeliveredByCourier(courierId, second);
            assertThat(statusOf(courierId)).isEqualTo(CourierStatus.IDLE);
        } finally {
            // Leave no idle courier or waiting order behind for other tests' dispatch runs
            orders.findById(second).filter(o -> o.getStatus() == OrderStatus.READY && o.getCourierId() != null)
                    .ifPresent(o -> {
                        var assignment = assignments.findByOrderIdAndStatusIn(second, AssignmentService.ACTIVE).orElseThrow();
                        if (assignment.getStatus() == AssignmentStatus.OFFERED) courierDutyService.acceptOffer(courierId, second);
                        orderService.markPickedUpByCourier(courierId, second);
                        orderService.markDeliveredByCourier(courierId, second);
                    });
            if (statusOf(courierId) == CourierStatus.IDLE) courierDutyService.goOffDuty(courierId);
            jdbc.update("DELETE FROM assignments WHERE order_id IN (?, ?)", first, second);
            orders.deleteById(first);
            orders.deleteById(second);
        }
    }
}
