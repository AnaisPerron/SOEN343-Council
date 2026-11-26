package com.council.tenderapp.ord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.dlv.services.AssignmentService;
import com.council.tenderapp.dlv.services.DispatchService;
import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderLineRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.services.OrderCancellationService;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.ord.services.OrderStatusPublisher;
import com.council.tenderapp.ord.services.PreparationQueueService;
import com.council.tenderapp.prc.service.LedgerService;
import com.council.tenderapp.prc.service.OrderBelongingService;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.technicalservices.payment.TenderCash;

/**
 * Characterization tests (R4): pin down who may act on an order today, in every class that
 * repeats an ownership check, before those checks are moved into one place.
 */
@ExtendWith(MockitoExtension.class)
class OrderAccessCharacterizationTest {
    @Mock OrderRepository orderRepository;
    @Mock OrderStatusPublisher orderStatusPublisher;
    @Mock OrderEventRepository orderEventRepository;
    @Mock ProfileRepository profileRepository;
    @Mock OrderLineRepository orderLineRepository;
    @Mock TenderCash tenderCash;
    @Mock CourierProfileRepository courierProfileRepository;
    @Mock LedgerService ledgerService;
    @Mock AssignmentService assignments;
    @Mock DispatchService dispatch;
    @Mock AssignmentRepository assignmentHistory;
    @Mock PreparationQueueService preparationQueue;

    @InjectMocks OrderService orderService;

    private static Order order(long id, Long customerId, Long restaurantId, Long courierId, OrderStatus status) {
        Order order = new Order(customerId, restaurantId, status, BigDecimal.TEN);
        order.setId(id);
        order.setCourierId(courierId);
        order.setDateOrdered(new Date(1_000L));
        return order;
    }

    // ---- courier ownership in OrderService ----

    @Test
    void pickupByAnotherCourierIsForbiddenAndChangesNothing() {
        Order ready = order(13L, 1L, 42L, 77L, OrderStatus.READY);
        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(ready));

        assertThatThrownBy(() -> orderService.markPickedUpByCourier(78L, 13L))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(e.getReason()).isEqualTo("Order is not assigned to this courier");
            });

        assertThat(ready.getStatus()).isEqualTo(OrderStatus.READY);
        verify(assignments, never()).pickUp(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deliveryByAnotherCourierIsForbiddenAndChangesNothing() {
        Order pickedUp = order(14L, 1L, 42L, 77L, OrderStatus.PICKED_UP);
        when(orderRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(pickedUp));

        assertThatThrownBy(() -> orderService.markDeliveredByCourier(78L, 14L))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(e.getReason()).isEqualTo("Order is not assigned to this courier");
            });

        assertThat(pickedUp.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        verify(assignments, never()).current(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void courierActionOnUnassignedOrderIsForbidden() {
        Order ready = order(15L, 1L, 42L, null, OrderStatus.READY);
        when(orderRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(ready));

        assertThatThrownBy(() -> orderService.markPickedUpByCourier(77L, 15L))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void courierActionOnMissingOrderIsNotFound() {
        when(orderRepository.findByIdForUpdate(16L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.markDeliveredByCourier(77L, 16L))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(e.getReason()).isEqualTo("Order not found");
            });
    }

    // ---- restaurant ownership in OrderService ----

    @Test
    void restaurantActionOnAnotherRestaurantsOrderIsBadRequest() {
        Order delivered = order(17L, 1L, 99L, 77L, OrderStatus.DELIVERED);
        when(orderRepository.findByIdForUpdate(17L)).thenReturn(Optional.of(delivered));

        assertThatThrownBy(() -> orderService.refundOrder(42L, 17L))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(e.getReason()).isEqualTo("Order does not belong to the restaurant");
            });

        assertThat(delivered.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(tenderCash, never()).refunded(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void restaurantActionOnMissingOrderIsNotFound() {
        when(orderRepository.findByIdForUpdate(18L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.rejectOrder(42L, 18L, "closed"))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---- customer ownership in OrderCancellationService ----

    private OrderCancellationService cancellations() {
        return new OrderCancellationService(orderRepository, preparationQueue, orderStatusPublisher, tenderCash);
    }

    @Test
    void customerCannotCancelAnotherCustomersOrder() {
        Order placed = order(20L, 5L, 42L, null, OrderStatus.PLACED);
        when(orderRepository.findWithLockById(20L)).thenReturn(Optional.of(placed));

        assertThatThrownBy(() -> cancellations().cancelForCustomer(6L, 20L))
            .isInstanceOf(OrderAccessDeniedException.class)
            .hasMessage("Order 20 does not belong to this customer");

        assertThat(placed.getStatus()).isEqualTo(OrderStatus.PLACED);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void missingCustomerIdIsDeniedEvenForAnOrderWithoutCustomer() {
        Order placed = order(21L, null, 42L, null, OrderStatus.PLACED);
        when(orderRepository.findWithLockById(21L)).thenReturn(Optional.of(placed));

        assertThatThrownBy(() -> cancellations().cancelForCustomer(null, 21L))
            .isInstanceOf(OrderAccessDeniedException.class)
            .hasMessage("Order 21 does not belong to this customer");
    }

    @Test
    void ownerCancelsTheirPlacedOrder() {
        Order placed = order(22L, 5L, 42L, null, OrderStatus.PLACED);
        when(orderRepository.findWithLockById(22L)).thenReturn(Optional.of(placed));

        OrderStatusResponseDto response = cancellations().cancelForCustomer(5L, 22L);

        assertThat(response.status()).isEqualTo("cancelled");
        assertThat(placed.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(tenderCash).refunded(placed);
    }

    // ---- read checks in OrderBelongingService ----

    @Test
    void belongingChecksMatchCourierAndRestaurantAndCustomer() {
        Order order = order(30L, 5L, 42L, 77L, OrderStatus.READY);
        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));
        OrderBelongingService belonging = new OrderBelongingService(orderRepository);

        assertThat(belonging.isCourierOrder(30L, 77L)).isTrue();
        assertThat(belonging.isCourierOrder(30L, 78L)).isFalse();
        assertThat(belonging.isCourierOrder(30L, null)).isFalse();
        assertThat(belonging.isRestaurantOrder(30L, 42L)).isTrue();
        assertThat(belonging.isRestaurantOrder(30L, 43L)).isFalse();
        assertThat(belonging.isCustomerOrder(30L, 5L)).isTrue();
        assertThat(belonging.isCustomerOrder(30L, 6L)).isFalse();
    }

    @Test
    void belongingChecksRefuseMissingOrders() {
        when(orderRepository.findById(31L)).thenReturn(Optional.empty());
        OrderBelongingService belonging = new OrderBelongingService(orderRepository);

        assertThat(belonging.isCourierOrder(31L, 77L)).isFalse();
        assertThat(belonging.isRestaurantOrder(31L, 42L)).isFalse();
        assertThat(belonging.isCustomerOrder(31L, 5L)).isFalse();
    }

    @Test
    void belongingCheckTreatsTwoMissingIdsAsAMatch() {
        // Current behaviour: Objects.equals(null, null) is true for an order with no courier.
        Order order = order(32L, 5L, 42L, null, OrderStatus.PLACED);
        when(orderRepository.findById(32L)).thenReturn(Optional.of(order));
        OrderBelongingService belonging = new OrderBelongingService(orderRepository);

        assertThat(belonging.isCourierOrder(32L, null)).isTrue();
    }
}
