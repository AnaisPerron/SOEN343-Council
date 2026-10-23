package com.council.tenderapp.ORD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.council.tenderapp.ORD.persistence.Order;
import com.council.tenderapp.ORD.persistence.OrderItem;
import com.council.tenderapp.ORD.persistence.OrderRepository;
import com.council.tenderapp.ORD.persistence.OrderResponse;
import com.council.tenderapp.ORD.persistence.OrderStatus;
import com.council.tenderapp.ORD.services.OrderService;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void preparationQueueIncludesAcceptedAndPreparingOrdersForRestaurant() {
        Long restaurantId = 42L;
        Date acceptedAt = new Date(1_700_000_000_000L);
        Order accepted = new Order(restaurantId, new Date(), OrderStatus.Accepted, 12.50);
        accepted.setId(1L);
        accepted.setAcceptedAt(acceptedAt);
        accepted.addItem(new OrderItem("Soup", 2));

        Order preparing = new Order(restaurantId, new Date(), OrderStatus.Preparing, 8.00);
        preparing.setId(2L);
        preparing.setAcceptedAt(new Date(acceptedAt.getTime() + 1_000L));
        preparing.addItem(new OrderItem("Salad", 1));

        when(orderRepository.findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
            restaurantId,
            List.of(OrderStatus.Accepted, OrderStatus.Preparing)
        )).thenReturn(List.of(accepted, preparing));

        List<OrderResponse> result = orderService.getPreparationQueue(restaurantId);

        verify(orderRepository).findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
            restaurantId,
            List.of(OrderStatus.Accepted, OrderStatus.Preparing)
        );
        assertThat(result)
            .extracting(OrderResponse::getId)
            .containsExactly(1L, 2L);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.Accepted);
        assertThat(result.get(0).getAcceptedAt()).isEqualTo(acceptedAt);
        assertThat(result.get(0).getItems())
            .extracting("itemName")
            .containsExactly("Soup");
        assertThat(result.get(0).getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void enteringAcceptedStatusRecordsAcceptanceTime() {
        Order order = new Order();

        order.setStatus(OrderStatus.Accepted);

        assertThat(order.getAcceptedAt()).isNotNull();
    }
}
