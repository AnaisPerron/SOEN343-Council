package com.council.tenderapp.dlv.services;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderService;


@Service
public class CourierDeliveryService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderService orderService;
    private final AssignmentService assignments;

    public CourierDeliveryService(OrderRepository orderRepository,
            RestaurantRepository restaurantRepository, OrderService orderService, AssignmentService assignments) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderService = orderService;
        this.assignments = assignments;
    }

    @Transactional
    public AssignedOrderDto confirmPickup(Long courierId, Long orderId) {
        assignments.lockCouriers();
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!courierId.equals(order.getCourierId())) {
            throw new OrderAccessDeniedException("Order " + orderId + " is not assigned to this courier");
        }

        if (order.getStatus() != OrderStatus.READY) {
            throw new InvalidOrderStateException("Order " + orderId + " cannot be picked up because it is "
                    + order.getStatus().name().toLowerCase(Locale.ROOT));
        }

        var assignment = assignments.current(order).orElseThrow(() -> new InvalidOrderStateException("Order has no courier assignment"));
        orderService.markPickedUpByCourier(courierId, orderId);

        return new AssignedOrderDto(order, restaurantRepository.findById(order.getRestaurantId()).orElse(null), assignment);
    }

    @Transactional
    public AssignedOrderDto confirmDelivery(Long courierId, Long orderId) {
        assignments.lockCouriers();
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!courierId.equals(order.getCourierId())) {
            throw new OrderAccessDeniedException("Order " + orderId + " is not assigned to this courier");
        }
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new InvalidOrderStateException("Order " + orderId + " cannot be delivered because it is "
                    + order.getStatus().name().toLowerCase(Locale.ROOT));
        }
        var assignment = assignments.current(order).orElseThrow(() -> new InvalidOrderStateException("Order has no courier assignment"));
        orderService.markDeliveredByCourier(courierId, orderId);

        return new AssignedOrderDto(order, restaurantRepository.findById(order.getRestaurantId()).orElse(null), assignment);
    }
}
