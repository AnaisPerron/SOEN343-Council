package com.council.tenderapp.ord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.ord.dtos.OrderResponse;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderLine;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderLineRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.ord.services.OrderStatusPublisher;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.prc.service.LedgerService;
import com.council.tenderapp.technicalservices.payment.TenderCash;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusPublisher orderStatusPublisher;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private TenderCash tenderCash;

    @Mock
    private CourierProfileRepository courierProfileRepository;

    @Mock
    private LedgerService ledgerService;

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
    void restaurantOrdersIncludeCustomerNameAndPhone() {
        Long restaurantId = 42L;
        Order placed = new Order(21L, restaurantId, OrderStatus.PLACED, new BigDecimal("17.00"));
        placed.setId(5L);
        Order guest = order(restaurantId, OrderStatus.PLACED, new Date(2000L));
        guest.setId(6L);
        Profile customer = new Profile(Role.CUSTOMER, "Alice Martin", "alice@demo.test", "hash", "514-555-0101");
        ReflectionTestUtils.setField(customer, "id", 21L);
        when(orderRepository.findByRestaurantIdOrderByDateOrderedAscIdAsc(restaurantId))
            .thenReturn(List.of(placed, guest));
        when(profileRepository.findAllById(List.of(21L))).thenReturn(List.of(customer));

        List<OrderResponse> result = orderService.getAllOrders(restaurantId);

        assertThat(result.get(0).getCustomerName()).isEqualTo("Alice Martin");
        assertThat(result.get(0).getCustomerPhone()).isEqualTo("514-555-0101");
        // Seeded orders without a customer have no name to show.
        assertThat(result.get(1).getCustomerName()).isNull();
    }

    @Test
    void restaurantOrdersShowCourierNameOnlyAfterOfferAcceptance() {
        Long restaurantId = 42L;
        Long courierId = 77L;
        Order ready = order(restaurantId, OrderStatus.READY, new Date());
        ready.setId(7L);
        ready.setCourierId(courierId);
        Profile courierProfile = new Profile(Role.COURIER, "Jordan Courier", "jordan@example.test", "hash", "514-555-0101");
        ReflectionTestUtils.setField(courierProfile, "id", courierId);
        CourierProfile duty = new CourierProfile(courierId);
        duty.setStatus(CourierStatus.IDLE);
        when(orderRepository.findByRestaurantIdOrderByDateOrderedAscIdAsc(restaurantId)).thenReturn(List.of(ready));
        when(profileRepository.findAllById(List.of(courierId))).thenReturn(List.of(courierProfile));
        when(courierProfileRepository.findAllById(List.of(courierId))).thenReturn(List.of(duty));

        assertThat(orderService.getAllOrders(restaurantId).get(0).getCourierName()).isNull();

        duty.setStatus(CourierStatus.ASSIGNED);
        assertThat(orderService.getAllOrders(restaurantId).get(0).getCourierName()).isEqualTo("Jordan Courier");
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
        verify(orderStatusPublisher).publish(placed, OrderStatus.PLACED, "accepted", null);
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
    void acceptsRejectionWithoutAReason() {
        // The frontend allows the restaurant to leave the reason empty.
        Order placed = order(42L, OrderStatus.PLACED, new Date());
        placed.setId(6L);
        when(orderRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(placed));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrderResponse rejected = orderService.rejectOrder(42L, 6L, "  ");
        assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isNull();
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
    void refusesToPrepareANewerOrderWhileAnOlderAcceptedOrderWaits() {
        Order older = order(42L, OrderStatus.ACCEPTED, new Date(1000L));
        older.setId(20L);
        Order newer = order(42L, OrderStatus.ACCEPTED, new Date(2000L));
        newer.setId(21L);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(newer));
        when(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(42L, OrderStatus.ACCEPTED))
            .thenReturn(List.of(older, newer));

        assertThatThrownBy(() -> orderService.startPreparation(42L, 21L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Start preparing order #20 first");
        assertThat(newer.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(orderRepository, never()).save(any());
        verify(orderStatusPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void preparesTheOldestAcceptedOrder() {
        Order older = order(42L, OrderStatus.ACCEPTED, new Date(1000L));
        older.setId(20L);
        Order newer = order(42L, OrderStatus.ACCEPTED, new Date(2000L));
        newer.setId(21L);
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(older));
        when(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(42L, OrderStatus.ACCEPTED))
            .thenReturn(List.of(older, newer));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.startPreparation(42L, 20L).getStatus()).isEqualTo(OrderStatus.PREPARING);
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
    void courierAdvancesAssignedOrderFromReadyThroughDelivery() {
        Order ready = order(42L, OrderStatus.READY, new Date());
        ready.setId(13L);
        ready.setCourierId(77L);
        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(ready));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.markPickedUpByCourier(77L, 13L).getStatus()).isEqualTo(OrderStatus.PICKED_UP);

        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(ready));
        assertThat(orderService.markDeliveredByCourier(77L, 13L).getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void onlyAssignedCourierCanConfirmPickedUpDelivery() {
        Order order = order(42L, OrderStatus.PICKED_UP, new Date());
        order.setId(19L);
        order.setCourierId(6L);
        when(orderRepository.findByIdForUpdate(19L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> orderService.confirmDelivery(7L, 19L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(orderService.confirmDelivery(6L, 19L).getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDateDelivered()).isNotNull();
    }

    @Test
    void refundsOnlyDeliveredOrders() {
        for (OrderStatus currentStatus : List.of(OrderStatus.PLACED, OrderStatus.READY, OrderStatus.PICKED_UP,
                OrderStatus.REFUNDED)) {
            Order order = order(42L, currentStatus, new Date());
            order.setId(14L);
            when(orderRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(order));
            assertThatThrownBy(() -> orderService.refundOrder(42L, 14L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            verify(orderRepository, never()).save(any());
        }
        Order delivered = order(42L, OrderStatus.DELIVERED, new Date());
        delivered.setId(14L);
        when(orderRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(delivered));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(orderService.refundOrder(42L, 14L).getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(tenderCash).refunded(delivered);
        verify(ledgerService).createRefundEntry(delivered);
        org.mockito.ArgumentCaptor<com.council.tenderapp.ord.entities.OrderEvent> event =
                org.mockito.ArgumentCaptor.forClass(com.council.tenderapp.ord.entities.OrderEvent.class);
        verify(orderEventRepository).saveAndFlush(event.capture());
        assertThat(event.getValue().getOrderId()).isEqualTo(14L);
        assertThat(event.getValue().getPreviousStatus()).isEqualTo("delivered");
        assertThat(event.getValue().getNewStatus()).isEqualTo("refunded");
        assertThat(event.getValue().getEventType()).isEqualTo("order_refunded");

        assertThatThrownBy(() -> orderService.refundOrder(42L, 14L))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(orderRepository, org.mockito.Mockito.times(1)).save(any());
        verify(orderEventRepository, org.mockito.Mockito.times(1)).saveAndFlush(any());
        verify(ledgerService, org.mockito.Mockito.times(1)).createRefundEntry(delivered);
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
        verify(orderStatusPublisher, never()).publish(any(), any(), any(), any());
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
