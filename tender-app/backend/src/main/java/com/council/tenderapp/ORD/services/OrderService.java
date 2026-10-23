package com.council.tenderapp.ORD.services;

import com.council.tenderapp.ORD.persistence.Order;
import com.council.tenderapp.ORD.persistence.OrderItemResponse;
import com.council.tenderapp.ORD.persistence.OrderRepository;
import com.council.tenderapp.ORD.persistence.OrderResponse;
import com.council.tenderapp.ORD.persistence.OrderStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final List<OrderStatus> PREPARATION_STATUSES =
        List.of(OrderStatus.Accepted, OrderStatus.Preparing);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPreparationQueue(Long restaurantId) {
        return orderRepository
            .findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(restaurantId, PREPARATION_STATUSES)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderItemResponse(item.getItemName(), item.getQuantity()))
            .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getAcceptedAt(), items);
    }
}
