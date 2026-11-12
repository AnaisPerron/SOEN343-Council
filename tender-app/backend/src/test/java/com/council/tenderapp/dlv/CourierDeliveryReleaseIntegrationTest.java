package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.time.Instant;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.dlv.services.AssignmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
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
    @Autowired private AssignmentRepository assignments;
    @Autowired private PlatformTransactionManager transactions;

    private CourierStatus statusOf(Long courierId) {
        return couriers.findById(courierId).orElseThrow().getStatus();
    }

    @Test
    void courierReturnsToIdleAfterDeliveringAndTakesTheNextWaitingOrder() {
        Long courierId = registrationService.register(new RegisterRequest(
                "Release Tester", "release-courier@example.com", "secret123", "514-555-0180", Role.COURIER)).id();
        Long restaurantId = restaurants.saveAndFlush(new Restaurant("Release Cafe", "open", 45.50, -73.57)).getId();
        Long first = orders.saveAndFlush(new Order(restaurantId, new Date(System.currentTimeMillis() - 60_000), OrderStatus.READY)).getId();
        Long second = orders.saveAndFlush(new Order(restaurantId, new Date(), OrderStatus.READY)).getId();
        try {
            courierDutyService.goOnDuty(courierId);
            assertThat(orders.findById(first).orElseThrow().getCourierId()).isEqualTo(courierId);
            assertThat(orders.findById(second).orElseThrow().getCourierId()).isNull();

            new TransactionTemplate(transactions).executeWithoutResult(tx -> assignments
                    .findByOrderIdAndStatusIn(first, AssignmentService.ACTIVE).orElseThrow().accept(Instant.now()));
            orderService.markPickedUp(restaurantId, first);
            assertThat(statusOf(courierId)).isEqualTo(CourierStatus.CARRYING);

            orderService.markDelivered(restaurantId, first);

            // Freed by the delivery, then immediately handed the order that was waiting
            assertThat(orders.findById(second).orElseThrow().getCourierId()).isEqualTo(courierId);
            assertThat(statusOf(courierId)).isEqualTo(CourierStatus.ASSIGNED);

            orderService.refundOrder(restaurantId, second);
            assertThat(statusOf(courierId)).isEqualTo(CourierStatus.IDLE);
        } finally {
            // Leave no idle courier or waiting order behind for other tests' dispatch runs
            orders.findById(second).filter(o -> o.getStatus() == OrderStatus.READY)
                    .ifPresent(o -> orderService.refundOrder(restaurantId, second));
            if (statusOf(courierId) == CourierStatus.IDLE) courierDutyService.goOffDuty(courierId);
        }
    }
}
