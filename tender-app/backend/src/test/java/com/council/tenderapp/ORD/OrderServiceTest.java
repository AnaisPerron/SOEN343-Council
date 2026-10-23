package com.council.tenderapp.ORD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.council.tenderapp.ORD.entities.MenuItem;
import com.council.tenderapp.ORD.entities.Order;
import com.council.tenderapp.ORD.entities.OrderLine;
import com.council.tenderapp.ORD.entities.OrderStatus;
import com.council.tenderapp.ORD.dto.response.OrderResponse;
import com.council.tenderapp.ORD.repositories.OrderRepository;
import com.council.tenderapp.ORD.services.OrderService;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void incomingQueueFiltersByRestaurantAndMapsOrderLines() {
        Long restaurantId = 42L;
        Date placedAt = new Date(1_700_000_000_000L);
        Order placed = order(restaurantId, OrderStatus.PLACED, placedAt);
        placed.setId(3L);
        placed.addOrderLine(new OrderLine(menuItem(3L, "Pasta"), 3, new BigDecimal("7.25")));
        when(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId,
            OrderStatus.PLACED
        )).thenReturn(List.of(placed));

        List<OrderResponse> result = orderService.getIncomingOrders(restaurantId);

        verify(orderRepository).findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId,
            OrderStatus.PLACED
        );
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderLines()).hasSize(1);
        assertThat(result.get(0).getOrderLines().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void preparationQueueIncludesAcceptedAndPreparingStatusesForRestaurant() {
        Long restaurantId = 42L;
        Order accepted = order(restaurantId, OrderStatus.ACCEPTED, new Date(1000L));
        accepted.setId(1L);
        accepted.setAcceptedAt(new Date(1000L));
        accepted.addOrderLine(new OrderLine(menuItem(1L, "Soup"), 2, new BigDecimal("6.25")));
        Order preparing = order(restaurantId, OrderStatus.PREPARING, new Date(2000L));
        preparing.setId(2L);
        preparing.setAcceptedAt(new Date(2000L));
        when(orderRepository.findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
            restaurantId,
            List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING)
        )).thenReturn(List.of(accepted, preparing));

        List<OrderResponse> result = orderService.getPreparationQueue(restaurantId);

        verify(orderRepository).findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
            restaurantId,
            List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING)
        );
        assertThat(result).extracting(OrderResponse::getId).containsExactly(1L, 2L);
        assertThat(result.get(0).getTotalPrice()).isEqualTo(new BigDecimal("12.50"));
    }

    @Test
    void pickupQueueIncludesReadyOrdersForRestaurant() {
        Long restaurantId = 42L;
        Order ready = order(restaurantId, OrderStatus.READY, new Date(1_000L));
        ready.setId(12L);
        when(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId,
            OrderStatus.READY
        )).thenReturn(List.of(ready));

        List<OrderResponse> result = orderService.getPickupQueue(restaurantId);

        verify(orderRepository).findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId,
            OrderStatus.READY
        );
        assertThat(result).extracting(OrderResponse::getId).containsExactly(12L);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void acceptsPlacedOrderBeforePreparationStarts() {
        Order placed = order(42L, OrderStatus.PLACED, new Date());
        placed.setId(5L);
        when(orderRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(placed));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse accepted = orderService.acceptOrder(42L, 5L);

        assertThat(accepted.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(accepted.getAcceptedAt()).isNotNull();
    }

    @Test
    void rejectsPlacedOrderWithTrimmedReason() {
        Order placed = order(42L, OrderStatus.PLACED, new Date());
        placed.setId(6L);
        when(orderRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(placed));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse rejected = orderService.rejectOrder(42L, 6L, "  Sold out  ");

        assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Sold out");
    }

    @Test
    void rejectsBlankReasonWithoutChangingOrSavingOrder() {
        assertThatThrownBy(() -> orderService.rejectOrder(42L, 6L, "  "))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("A rejection reason is required");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void advancesOnlyThroughPreparingAndReady() {
        Order accepted = order(42L, OrderStatus.ACCEPTED, new Date());
        accepted.setId(7L);
        when(orderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(accepted));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.startPreparation(42L, 7L).getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void marksPreparingOrderReady() {
        Order preparing = order(42L, OrderStatus.PREPARING, new Date());
        preparing.setId(8L);
        when(orderRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(preparing));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.markReady(42L, 8L).getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void advancesReadyOrderToPickedUpAndDelivered() {
        Order ready = order(42L, OrderStatus.READY, new Date());
        ready.setId(13L);
        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(ready));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.markPickedUp(42L, 13L).getStatus()).isEqualTo(OrderStatus.PICKED_UP);

        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(ready));
        assertThat(orderService.markDelivered(42L, 13L).getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void refundsReadyPickedUpOrDeliveredOrders() {
        for (OrderStatus currentStatus : List.of(
            OrderStatus.READY,
            OrderStatus.PICKED_UP,
            OrderStatus.DELIVERED
        )) {
            Order order = order(42L, currentStatus, new Date());
            order.setId(14L);
            when(orderRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThat(orderService.refundOrder(42L, 14L).getStatus()).isEqualTo(OrderStatus.REFUNDED);
        }
    }

    @Test
    void cancelsPlacedOrder() {
        Order placed = order(42L, OrderStatus.PLACED, new Date());
        placed.setId(9L);
        when(orderRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(placed));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.cancelOrder(42L, 9L).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void rejectsInvalidTransitionWithoutSaving() {
        Order placed = order(42L, OrderStatus.PLACED, new Date());
        placed.setId(10L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(placed));

        assertThatThrownBy(() -> orderService.startPreparation(42L, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid order transition");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void preventsActionOnOrderOwnedByAnotherRestaurant() {
        Order placed = order(99L, OrderStatus.PLACED, new Date());
        placed.setId(11L);
        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(placed));

        assertThatThrownBy(() -> orderService.acceptOrder(42L, 11L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("does not belong to the restaurant");

        verify(orderRepository, never()).save(any());
    }

    private Order order(Long restaurantId, OrderStatus status, Date createdAt) {
        return new Order(restaurantId, createdAt, status);
    }

    private MenuItem menuItem(Long id, String name) {
        return new MenuItem(id, name, BigDecimal.ONE);
    }
}
