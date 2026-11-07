package com.council.tenderapp.ord.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.council.tenderapp.ord.dtos.OrderItemRequestDto;
import com.council.tenderapp.ord.dtos.OrderResponseDto;
import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.MenuItemOption;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.MenuItemUnavailableException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.exceptions.RestaurantClosedException;
import com.council.tenderapp.ord.repositories.MenuItemOptionRepository;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.reg.services.CustomerProfileService;
import com.council.tenderapp.technicalservices.payment.PaymentDeclinedException;
import com.council.tenderapp.technicalservices.payment.PaymentGateway;
import com.council.tenderapp.technicalservices.payment.PaymentResult;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long RESTAURANT_ID = 1L;

    @Mock
    private BrowseService browseService;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private MenuItemOptionRepository menuItemOptionRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PreparationQueueService preparationQueueService;
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private CustomerProfileService customerProfileService;

    @Mock
    private OrderStatusPublisher publisher;

    private CustomerOrderService service;

    @BeforeEach
    void setUp() {
        service = new CustomerOrderService(browseService, menuItemRepository, menuItemOptionRepository, orderRepository, paymentGateway, preparationQueueService, jdbc, customerProfileService,
                new OrderCancellationService(orderRepository, preparationQueueService, publisher));
    }

    private static MenuItem item(Long id, String name, String price, String status) {
        return new MenuItem(id, 1L, new BigDecimal(price), status, null, name, null, null);
    }

    private void menu(MenuItem... items) {
        when(menuItemRepository.findMenuItemsByRestaurantId(RESTAURANT_ID)).thenReturn(List.of(items));
    }

    private static PlaceOrderRequestDto request(OrderItemRequestDto... items) {
        return new PlaceOrderRequestDto(RESTAURANT_ID, List.of(items));
    }

    @Test
    void placesOrderComputesTotalAndQueuesIt() {
        menu(item(1L, "Margherita Pizza", "14.00", "available"), item(4L, "Sparkling Water", "3.00", "available"));
        when(paymentGateway.charge(CUSTOMER_ID, new BigDecimal("34.00"))).thenReturn(PaymentResult.approved("MOCK-1"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 42L);
            return order;
        });

        OrderResponseDto response = service.placeOrder(CUSTOMER_ID,
                request(new OrderItemRequestDto(1L, 2), new OrderItemRequestDto(4L, 2)));

        assertThat(response.orderId()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo("placed");
        assertThat(response.total()).isEqualByComparingTo("34.00");
        assertThat(response.paymentReference()).isEqualTo("MOCK-1");
        assertThat(response.lines()).hasSize(2);
        assertThat(response.lines().get(0).lineTotal()).isEqualByComparingTo("28.00");

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.getValue().getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(saved.getValue().getTotalPrice()).isEqualByComparingTo("34.00");
        verify(preparationQueueService).enqueue(eq(saved.getValue()), any(Instant.class));
        verify(customerProfileService).ensureExists(CUSTOMER_ID);
    }

    @Test
    void storesMenuPriceSnapshotOnEachLine() {
        menu(item(2L, "Pepperoni Pizza", "17.00", "available"));
        when(paymentGateway.charge(any(), any())).thenReturn(PaymentResult.approved("MOCK-1"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        service.placeOrder(CUSTOMER_ID, request(new OrderItemRequestDto(2L, 3)));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getOrderLines()).singleElement().satisfies(line -> {
            assertThat(line.getMenuItemId()).isEqualTo(2L);
            assertThat(line.getQuantity()).isEqualTo(3);
            assertThat(line.getUnitPrice()).isEqualByComparingTo("17.00");
        });
    }

    @Test
    void rejectsClosedRestaurant() {
        doThrow(new RestaurantClosedException("closed")).when(browseService).checkRestaurantOpen(RESTAURANT_ID);

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, request(new OrderItemRequestDto(1L, 1))))
                .isInstanceOf(RestaurantClosedException.class);
        verifyNoInteractions(paymentGateway, orderRepository, preparationQueueService);
    }

    @Test
    void rejectsItemFromAnotherRestaurant() {
        menu(item(1L, "Margherita Pizza", "14.00", "available"));

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, request(new OrderItemRequestDto(5L, 1))))
                .isInstanceOf(InvalidOrderException.class);
        verifyNoInteractions(paymentGateway, orderRepository, preparationQueueService);
    }

    @Test
    void rejectsSoldOutItem() {
        menu(item(3L, "Truffle Pizza", "22.00", "sold_out"));

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, request(new OrderItemRequestDto(3L, 1))))
                .isInstanceOf(MenuItemUnavailableException.class);
        verifyNoInteractions(paymentGateway, orderRepository, preparationQueueService);
    }

    @Test
    void declinedPaymentCreatesNoOrder() {
        menu(item(1L, "Margherita Pizza", "14.00", "available"));
        when(paymentGateway.charge(any(), any())).thenReturn(PaymentResult.declined("Insufficient funds"));

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, request(new OrderItemRequestDto(1L, 1))))
                .isInstanceOf(PaymentDeclinedException.class);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(preparationQueueService);
    }

    private Order existingOrder(Long customerId, String status) {
        Order order = new Order(customerId, RESTAURANT_ID, OrderStatus.valueOf(status.toUpperCase()), new BigDecimal("14.00"));
        ReflectionTestUtils.setField(order, "id", 42L);
        when(orderRepository.findWithLockById(42L)).thenReturn(Optional.of(order));
        return order;
    }

    @Test
    void cancelsPlacedOrderAndRemovesItFromQueue() {
        Order order = existingOrder(CUSTOMER_ID, "placed");

        OrderStatusResponseDto response = service.cancelOrder(CUSTOMER_ID, 42L);

        assertThat(response.orderId()).isEqualTo(42L);
        assertThat(response.previousStatus()).isEqualTo("placed");
        assertThat(response.status()).isEqualTo("cancelled");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
        verify(preparationQueueService).remove(eq(order), eq("placed"), any(Instant.class));
    }

    @Test
    void cancellingUnknownOrderIsNotFound() {
        when(orderRepository.findWithLockById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelOrder(CUSTOMER_ID, 99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cannotCancelAnotherCustomersOrder() {
        Order order = existingOrder(2L, "placed");

        assertThatThrownBy(() -> service.cancelOrder(CUSTOMER_ID, 42L))
                .isInstanceOf(OrderAccessDeniedException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(preparationQueueService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"accepted", "preparing", "ready", "picked_up", "delivered", "rejected", "cancelled"})
    void cannotCancelOrderThatIsNotPlaced(String status) {
        Order order = existingOrder(CUSTOMER_ID, status);

        assertThatThrownBy(() -> service.cancelOrder(CUSTOMER_ID, 42L))
                .isInstanceOf(InvalidOrderStateException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.valueOf(status.toUpperCase()));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(preparationQueueService);
    }

    @Test
    void chargesSelectedOptionsAndRecordsThemPerLine() {
        menu(item(1L, "Margherita Pizza", "14.00", "available"));
        when(menuItemOptionRepository.findByMenuItemIdIn(List.of(1L))).thenReturn(List.of(
                new MenuItemOption(7L, 1L, new BigDecimal("2.00"), "Extra cheese"),
                new MenuItemOption(8L, 1L, new BigDecimal("1.50"), "Extra basil")));
        when(paymentGateway.charge(CUSTOMER_ID, new BigDecimal("35.00"))).thenReturn(PaymentResult.approved("MOCK-1"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDto response = service.placeOrder(CUSTOMER_ID,
                request(new OrderItemRequestDto(1L, 2, List.of(7L, 8L, 7L))));

        assertThat(response.total()).isEqualByComparingTo("35.00");
        assertThat(response.lines().get(0).unitPrice()).isEqualByComparingTo("17.50");
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getOrderLines().get(0).getUnitPrice()).isEqualByComparingTo("14.00");
        // Lines must be flushed so they have ids before their add-ons are inserted.
        InOrder writes = inOrder(orderRepository, jdbc);
        writes.verify(orderRepository).flush();
        writes.verify(jdbc).update(any(String.class), any(), eq(1L), eq(7L), eq(new BigDecimal("2.00")));
        verify(jdbc).update(any(String.class), any(), eq(1L), eq(8L), eq(new BigDecimal("1.50")));
    }

    @Test
    void rejectsOptionThatBelongsToAnotherItem() {
        menu(item(1L, "Margherita Pizza", "14.00", "available"), item(4L, "Sparkling Water", "3.00", "available"));
        when(menuItemOptionRepository.findByMenuItemIdIn(List.of(4L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID,
                request(new OrderItemRequestDto(4L, 1, List.of(7L)))))
                .isInstanceOf(InvalidOrderException.class);
        verifyNoInteractions(paymentGateway, orderRepository, jdbc);
    }
}
