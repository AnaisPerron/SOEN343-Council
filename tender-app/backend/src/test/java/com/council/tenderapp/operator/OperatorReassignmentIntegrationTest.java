package com.council.tenderapp.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.operator.services.OperatorReassignmentService;
import com.council.tenderapp.operator.services.OperatorResetService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderStatusPublisher;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:operator-reassignment;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class OperatorReassignmentIntegrationTest {
    @Autowired private OperatorReassignmentService service;
    @Autowired private OperatorResetService resets;
    @Autowired private OrderRepository orders;
    @Autowired private AssignmentRepository assignments;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private OrderEventRepository events;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoSpyBean private DispatchService dispatch;
    @MockitoBean private OrderStatusPublisher publisher;
    private Order order;

    private static void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                8L, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @BeforeEach
    void setUp() {
        authenticate("OPERATOR");
        var restaurant = restaurants.saveAndFlush(new Restaurant("Reassignment", "open", 45.5, -73.5));
        courier(101L, CourierStatus.ASSIGNED);
        order = new Order(restaurant.getId(), new Date(), OrderStatus.READY);
        order.setCourierId(101L);
        order = orders.saveAndFlush(order);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        assignments.deleteAllInBatch();
        events.deleteAllInBatch();
        orders.deleteAllInBatch();
        couriers.deleteAllInBatch();
        restaurants.deleteAllInBatch();
    }

    private void courier(long id, CourierStatus status) {
        var courier = new CourierProfile(id);
        courier.setStatus(status);
        couriers.saveAndFlush(courier);
    }

    @Test
    void reassignsToIdleCourierAndRecordsAnEventWithoutChangingOrderStatus() {
        courier(102L, CourierStatus.IDLE);
        service.reassign(order.getId(), 101L);
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isEqualTo(102L);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(couriers.findById(101L).orElseThrow().getStatus()).isEqualTo(CourierStatus.OFFLINE);
        assertThat(couriers.findById(102L).orElseThrow().getStatus()).isEqualTo(CourierStatus.ASSIGNED);
        assertThat(events.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getOrderId()).isEqualTo(order.getId());
            assertThat(event.getEventType()).isEqualTo("courier_reassigned");
            assertThat(event.getPreviousStatus()).isEqualTo("ready");
            assertThat(event.getNewStatus()).isEqualTo("ready");
        });
        verify(publisher).publish(org.mockito.ArgumentMatchers.any(Order.class),
                org.mockito.ArgumentMatchers.eq(OrderStatus.READY), org.mockito.ArgumentMatchers.eq("courier_reassigned"),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void keepsOrderQueuedWhenNoCourierIsAvailableAndLaterDispatchCanAssignIt() {
        service.reassign(order.getId(), 101L);
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isNull();
        courier(102L, CourierStatus.IDLE);
        dispatch.retryQueue();
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isEqualTo(102L);
    }

    @Test
    void alsoAcceptsAnAlreadyOfflineCourier() {
        courier(101L, CourierStatus.OFFLINE);
        service.reassign(order.getId(), 101L);
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "READY", mode = EnumSource.Mode.EXCLUDE)
    void rejectsEveryOrderStateOtherThanReady(OrderStatus status) {
        orders.deleteById(order.getId());
        order = new Order(order.getRestaurantId(), new Date(), status);
        order.setCourierId(101L);
        order = orders.saveAndFlush(order);
        assertThatThrownBy(() -> service.reassign(order.getId(), 101L)).isInstanceOf(InvalidOrderStateException.class);
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isEqualTo(101L);
        assertThat(events.count()).isZero();
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsMissingOrdersUnassignedOrdersAndStaleAssignments() {
        assertThatThrownBy(() -> service.reassign(Long.MAX_VALUE, 101L)).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> service.reassign(order.getId(), 999L)).isInstanceOf(InvalidOrderStateException.class);
        order.setCourierId(null);
        orders.saveAndFlush(order);
        assertThatThrownBy(() -> service.reassign(order.getId(), 101L)).isInstanceOf(InvalidOrderStateException.class);
        assertThat(events.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = CourierStatus.class, names = {"IDLE", "CARRYING"})
    void rejectsInconsistentCourierStates(CourierStatus status) {
        courier(101L, status);
        assertThatThrownBy(() -> service.reassign(order.getId(), 101L)).isInstanceOf(InvalidOrderStateException.class);
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isEqualTo(101L);
    }

    @Test
    void rejectsCouriersWithAnotherActiveOrder() {
        var other = new Order(order.getRestaurantId(), new Date(), OrderStatus.PICKED_UP);
        other.setCourierId(101L);
        orders.saveAndFlush(other);
        assertThatThrownBy(() -> service.reassign(order.getId(), 101L)).isInstanceOf(InvalidOrderStateException.class);
        assertThat(couriers.findById(101L).orElseThrow().getStatus()).isEqualTo(CourierStatus.ASSIGNED);
    }

    @Test
    void rollbackRestoresAssignmentCourierAndEventAndDoesNotDispatchOrPublish() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            service.reassign(order.getId(), 101L);
            verifyNoInteractions(publisher);
            tx.setRollbackOnly();
        });
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isEqualTo(101L);
        assertThat(couriers.findById(101L).orElseThrow().getStatus()).isEqualTo(CourierStatus.ASSIGNED);
        assertThat(events.count()).isZero();
        org.mockito.Mockito.verify(dispatch, org.mockito.Mockito.never()).retryQueueAfterCommit();
        verifyNoInteractions(publisher);
    }

    @Test
    void dispatchFailureLeavesACommittedQueuedOrderAndEvent() {
        doThrow(new IllegalStateException("dispatch unavailable")).when(dispatch).retryQueueAfterCommit();
        service.reassign(order.getId(), 101L);
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isNull();
        assertThat(events.count()).isEqualTo(1);
        assertThat(couriers.findById(101L).orElseThrow().getStatus()).isEqualTo(CourierStatus.OFFLINE);
    }

    @Test
    void serviceRejectsNonOperatorCallers() {
        authenticate("CUSTOMER");
        assertThatThrownBy(() -> service.reassign(order.getId(), 101L)).isInstanceOf(AccessDeniedException.class);
        assertThat(events.count()).isZero();
    }

    @Test
    void resetServiceAlsoRejectsNonOperatorsBeforeLoadingAnySeed() {
        authenticate("CUSTOMER");
        assertThatThrownBy(resets::reset).isInstanceOf(AccessDeniedException.class);
        assertThat(orders.findById(order.getId())).isPresent();
    }

    @Test
    void concurrentRetriesCannotEndTheReplacementAssignment() throws Exception {
        courier(102L, CourierStatus.IDLE);
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                authenticate("OPERATOR");
                try {
                    start.await(5, TimeUnit.SECONDS);
                    service.reassign(order.getId(), 101L);
                    return true;
                } catch (InvalidOrderStateException exception) {
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isEqualTo(102L);
            assertThat(events.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
