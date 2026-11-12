package com.council.tenderapp.dlv.services;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.council.tenderapp.dlv.dtos.AssignedOrderDto;
import com.council.tenderapp.dlv.entities.AssignmentStatus;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.CourierStatus;

@Service
public class CourierOfferService {
    private final AssignmentService lifecycle;
    private final AssignmentRepository assignments;
    private final OrderRepository orders;
    private final RestaurantRepository restaurants;

    public CourierOfferService(AssignmentService lifecycle, AssignmentRepository assignments,
            OrderRepository orders, RestaurantRepository restaurants) {
        this.lifecycle = lifecycle;
        this.assignments = assignments;
        this.orders = orders;
        this.restaurants = restaurants;
    }

    @Transactional
    @PreAuthorize("hasRole('COURIER') and #courierId == authentication.principal")
    public AssignedOrderDto accept(long courierId, long assignmentId) {
        var couriers = lifecycle.lockCouriers();
        var assignment = assignments.findWithLockById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (!Long.valueOf(courierId).equals(assignment.getCourierId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This offer belongs to another courier");
        }
        var order = orders.findWithLockById(assignment.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        var courier = couriers.stream().filter(c -> c.getUserId() == courierId).findFirst()
                .orElseThrow(() -> new InvalidOrderStateException("Courier is unavailable"));
        if (!assignment.getStatus().isActive() || order.getStatus() != OrderStatus.READY
                || !Long.valueOf(courierId).equals(order.getCourierId()) || courier.getStatus() != CourierStatus.ASSIGNED) {
            throw new InvalidOrderStateException("Offer is no longer available");
        }
        if (assignment.getStatus() == AssignmentStatus.OFFERED) assignment.accept(Instant.now());
        return new AssignedOrderDto(order, restaurants.findById(order.getRestaurantId()).orElse(null), assignment);
    }
}
