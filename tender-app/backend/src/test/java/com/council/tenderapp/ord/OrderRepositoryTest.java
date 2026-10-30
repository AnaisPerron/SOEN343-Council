package com.council.tenderapp.ord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void customerHistoryContainsOnlyTheCustomersOrders() {
        Order first = orderRepository.save(new Order(101L, 701L, OrderStatus.PLACED, BigDecimal.TEN));
        Order other = orderRepository.save(new Order(102L, 701L, OrderStatus.PLACED, BigDecimal.TEN));
        orderRepository.flush();
        assertThat(orderRepository.findByCustomerIdOrderByDateOrderedDescIdDesc(101L))
            .extracting(Order::getId).contains(first.getId()).doesNotContain(other.getId());
    }


    @Test
    void incomingOrdersArePlacedOnlyForRestaurantAndOrderedByArrivalTime() {
        Long restaurantId = 701L;
        Order newer = orderRepository.save(
            order(restaurantId, 2_000L, OrderStatus.PLACED)
        );
        Order otherRestaurant = orderRepository.save(
            order(702L, 500L, OrderStatus.PLACED)
        );
        Order older = orderRepository.save(order(restaurantId, 1_000L, OrderStatus.PLACED));
        for (OrderStatus status : OrderStatus.values()) {
            if (status != OrderStatus.PLACED) {
                orderRepository.save(order(restaurantId, 100L, status));
            }
        }
        orderRepository.flush();

        List<Order> result = orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId,
            OrderStatus.PLACED
        );

        assertThat(result)
            .extracting(Order::getId)
            .containsExactly(older.getId(), newer.getId());
        assertThat(result)
            .allMatch(order -> order.getRestaurantId().equals(restaurantId))
            .allMatch(order -> order.getStatus() == OrderStatus.PLACED)
            .extracting(Order::getDateOrdered)
            .isSorted();
        assertThat(result).noneMatch(order -> order.getId().equals(otherRestaurant.getId()));
    }

    @Test
    void incomingOrdersAreEmptyWhenRestaurantHasNoPlacedOrders() {
        orderRepository.save(order(703L, 1_000L, OrderStatus.ACCEPTED));

        assertThat(orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            703L,
            OrderStatus.PLACED
        )).isEmpty();
    }

    @Test
    void preparationQueueIncludesOnlyAcceptedAndPreparingOrdersForRestaurantInAcceptanceOrder() {
        Long restaurantId = 704L;
        Order newer = orderRepository.save(order(restaurantId, 2_000L, OrderStatus.ACCEPTED));
        newer.setAcceptedAt(new Date(2_000L));
        Order otherRestaurant = orderRepository.save(order(705L, 500L, OrderStatus.ACCEPTED));
        otherRestaurant.setAcceptedAt(new Date(500L));
        Order older = orderRepository.save(order(restaurantId, 1_000L, OrderStatus.PREPARING));
        older.setAcceptedAt(new Date(1_000L));
        orderRepository.save(order(restaurantId, 100L, OrderStatus.PLACED));
        orderRepository.save(order(restaurantId, 100L, OrderStatus.READY));
        orderRepository.flush();

        List<Order> result = orderRepository.findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
            restaurantId,
            List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING)
        );

        assertThat(result).extracting(Order::getId).containsExactly(older.getId(), newer.getId());
        assertThat(result)
            .allMatch(order -> order.getRestaurantId().equals(restaurantId))
            .extracting(Order::getAcceptedAt)
            .isSorted();
        assertThat(result).noneMatch(order -> order.getId().equals(otherRestaurant.getId()));
    }

    @Test
    void pickupQueueIncludesOnlyReadyOrdersForRestaurantInArrivalOrder() {
        Long restaurantId = 706L;
        Order newer = orderRepository.save(order(restaurantId, 2_000L, OrderStatus.READY));
        orderRepository.save(order(707L, 500L, OrderStatus.READY));
        Order older = orderRepository.save(order(restaurantId, 1_000L, OrderStatus.READY));
        orderRepository.save(order(restaurantId, 100L, OrderStatus.PREPARING));
        orderRepository.flush();

        List<Order> result = orderRepository.findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
            restaurantId,
            OrderStatus.READY
        );

        assertThat(result).extracting(Order::getId).containsExactly(older.getId(), newer.getId());
        assertThat(result).allMatch(order -> order.getRestaurantId().equals(restaurantId));
    }

    private Order order(Long restaurantId, long dateOrdered, OrderStatus status) {
        return new Order(restaurantId, new Date(dateOrdered), status);
    }
}
