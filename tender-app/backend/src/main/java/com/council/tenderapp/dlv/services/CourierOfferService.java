package com.council.tenderapp.dlv.services;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;

@Service
public class CourierOfferService {
    private final AssignmentService lifecycle;
    private final OrderRepository orders;
    private final RestaurantRepository restaurants;

    public CourierOfferService(AssignmentService lifecycle, OrderRepository orders, RestaurantRepository restaurants) {
        this.lifecycle = lifecycle;
        this.orders = orders;
        this.restaurants = restaurants;
    }

    @Transactional
    @PreAuthorize("hasRole('COURIER') and #courierId == authentication.principal")
    public AssignedOrderDto accept(long courierId, long assignmentId) {
        var assignment = lifecycle.accept(courierId, assignmentId);
        var order = orders.findById(assignment.getOrderId()).orElseThrow();
        return new AssignedOrderDto(order, restaurants.findById(order.getRestaurantId()).orElse(null), assignment);
    }
}
