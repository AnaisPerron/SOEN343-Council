package com.council.tenderapp.ord.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderRepository;

@SpringBootTest
class CustomerCancellationIntegrationTest {
    @Autowired private CustomerOrderService customerOrders;
    @Autowired private OrderRepository orders;
    @Autowired private OrderService restaurantOrders;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private OrderStatusPublisher publisher;
    @MockitoBean private com.council.tenderapp.technicalservices.payment.TenderCash tenderCash;

    private Long orderId;

    @BeforeEach
    void setUp() {
        authenticate("CUSTOMER");
        orderId = orders.saveAndFlush(new Order(1L, 901L, OrderStatus.PLACED, new BigDecimal("14.50"))).getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM order_events WHERE order_id = ?", orderId);
        orders.deleteById(orderId);
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private long eventCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM order_events WHERE order_id = ?", Long.class, orderId);
    }

    @Test
    void customerCancelsOwnOrderAndRecordsOneEventAfterCommit() {
        customerOrders.cancelOrder(1L, orderId);
        Order saved = orders.findById(orderId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(saved.getCustomerId()).isEqualTo(1);
        assertThat(saved.getRestaurantId()).isEqualTo(901);
        assertThat(jdbc.queryForObject("SELECT total_price FROM orders WHERE id = ?", BigDecimal.class, orderId))
                .isEqualByComparingTo("14.50");
        assertThat(restaurantOrders.getIncomingOrders(901L)).isEmpty();
        assertThat(eventCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT event_type FROM order_events WHERE order_id = ?", String.class, orderId))
                .isEqualTo("order_cancelled");
        verify(publisher).publish(argThat(o -> o.getId().equals(orderId)),
                eq(OrderStatus.PLACED), eq("cancelled"),
                isNull());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PLACED")
    void everyOtherOrderStateIsRejectedWithoutEvents(OrderStatus state) {
        jdbc.update("UPDATE orders SET status = ? WHERE id = ?", state.name().toLowerCase(Locale.ROOT), orderId);
        assertThatThrownBy(() -> customerOrders.cancelOrder(1L, orderId)).isInstanceOf(InvalidOrderStateException.class);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(state);
        assertThat(eventCount()).isZero();
        verifyNoInteractions(publisher);
    }

    @Test
    void repeatedCancellationDoesNotAddAnotherEvent() {
        customerOrders.cancelOrder(1L, orderId);
        assertThatThrownBy(() -> customerOrders.cancelOrder(1L, orderId)).isInstanceOf(InvalidOrderStateException.class);
        assertThat(eventCount()).isEqualTo(1);
        verify(publisher, times(1)).publish(any(), any(),
                anyString(), isNull());
    }

    @Test
    void rollbackRestoresStateAndSuppressesEventsAndNotifications() {
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            customerOrders.cancelOrder(1L, orderId);
            verifyNoInteractions(publisher);
            transaction.setRollbackOnly();
        });
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(eventCount()).isZero();
        verifyNoInteractions(publisher);
    }

    @Test
    void missingOrderIsNotFoundAndOperatorsCannotUseCustomerCancellation() {
        assertThatThrownBy(() -> customerOrders.cancelOrder(1L, Long.MAX_VALUE)).isInstanceOf(OrderNotFoundException.class);
        authenticate("OPERATOR");
        assertThatThrownBy(() -> customerOrders.cancelOrder(1L, orderId)).isInstanceOf(AccessDeniedException.class);
        assertThat(eventCount()).isZero();
    }

    @Test
    void customerCancellationStillEnforcesOwnership() {
        authenticate("CUSTOMER");
        assertThatThrownBy(() -> customerOrders.cancelOrder(2L, orderId)).isInstanceOf(OrderAccessDeniedException.class);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
        var response = customerOrders.cancelOrder(1L, orderId);
        assertThat(response.status()).isEqualTo("cancelled");
        assertThat(eventCount()).isEqualTo(1);
    }

    @Test
    void concurrentCancellationsProduceOneSuccessAndOneConflict() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            authenticate("CUSTOMER");
            try {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                customerOrders.cancelOrder(1L, orderId);
                return "cancelled";
            } catch (InvalidOrderStateException conflict) {
                return "conflict";
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("cancelled", "conflict");
            assertThat(eventCount()).isEqualTo(1);
            assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        } finally {
            executor.shutdownNow();
        }
    }
}
