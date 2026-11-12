package com.council.tenderapp.dlv.services;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderStatusPublisher;

@Service
public class CourierDeliveryService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderStatusPublisher orderStatusPublisher;

    public CourierDeliveryService(OrderRepository orderRepository, OrderEventRepository orderEventRepository,
            RestaurantRepository restaurantRepository, OrderStatusPublisher orderStatusPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderStatusPublisher = orderStatusPublisher;
    }

    @Transactional
    public AssignedOrderDto confirmPickup(Long courierId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!courierId.equals(order.getCourierId())) {
            throw new OrderAccessDeniedException("Order " + orderId + " is not assigned to this courier");
        }

        OrderStatus previous = order.getStatus();
        if (previous != OrderStatus.READY) {
            throw new InvalidOrderStateException("Order " + orderId + " cannot be picked up because it is "
                    + previous.name().toLowerCase(Locale.ROOT));
        }

        order.transitionTo(OrderStatus.PICKED_UP);
        orderRepository.save(order);

        orderEventRepository.save(new OrderEvent(orderId, "ready", "picked_up", "order_picked_up", Instant.now()));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                orderStatusPublisher.publish(order, previous, "picked up", null);
            }
        });

        return new AssignedOrderDto(order, restaurantRepository.findById(order.getRestaurantId()).orElse(null));
    }
}