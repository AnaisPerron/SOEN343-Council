package com.council.tenderapp.dlv.services;

import java.time.Instant;
import java.util.Optional;

import com.council.tenderapp.dlv.entities.Assignment;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

/**
 * Gives an assignment record to an older ready or picked-up order that has a courier but no active
 * assignment, and keeps the courier's reservation status in line with the order.
 * Extracted from {@link AssignmentService#current(Order)}.
 */
class AssignmentAdoption {
    private final AssignmentRepository assignments;
    private final CourierProfileRepository couriers;
    private final OrderEventRepository events;

    AssignmentAdoption(AssignmentRepository assignments, CourierProfileRepository couriers, OrderEventRepository events) {
        this.assignments = assignments;
        this.couriers = couriers;
        this.events = events;
    }

    Optional<Assignment> adopt(Order order) {
        if (order.getCourierId() == null || (order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.PICKED_UP)) {
            return Optional.empty();
        }
        if (assignments.existsByCourierIdAndStatusIn(order.getCourierId(), AssignmentService.ACTIVE)) {
            throw new InvalidOrderStateException("Courier already has another active assignment");
        }
        Instant now = Instant.now();
        Assignment adopted = new Assignment(order.getId(), order.getCourierId(), now);
        var courier = couriers.findWithLockByUserId(order.getCourierId()).orElseThrow();
        boolean alreadyAccepted = courier.getStatus() == CourierStatus.ASSIGNED
                && events.existsByOrderIdAndEventType(order.getId(), "courier_accepted");
        if (order.getStatus() == OrderStatus.PICKED_UP || alreadyAccepted) adopted.accept(now);
        synchronizeReservation(order);
        return Optional.of(assignments.saveAndFlush(adopted));
    }

    void synchronizeReservation(Order order) {
        couriers.findWithLockByUserId(order.getCourierId()).ifPresent(courier -> {
            if (courier.getStatus() == CourierStatus.IDLE) {
                courier.setStatus(order.getStatus() == OrderStatus.PICKED_UP ? CourierStatus.CARRYING : CourierStatus.ASSIGNED);
            }
        });
    }
}
