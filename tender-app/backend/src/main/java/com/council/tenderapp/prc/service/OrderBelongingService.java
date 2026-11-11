package com.council.tenderapp.prc.service;

import java.util.Optional;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.repositories.OrderRepository;

@Service
public class OrderBelongingService 
{
    
    private final OrderRepository orderRepository;

    public OrderBelongingService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public boolean isCustomerOrder(Long orderId, Long customerId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty() || !Objects.equals(order.get().getCustomerId(), customerId)) {
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isCourierOrder(Long orderId, Long courierId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty() || !Objects.equals(order.get().getCourierId(), courierId)) {
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isRestaurantOrder(Long orderId, Long restaurantId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty() || !Objects.equals(order.get().getRestaurantId(), restaurantId)) {
            return false;
        }
        return true;
    }
}
