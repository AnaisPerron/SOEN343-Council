package com.council.tenderapp.dlv.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import com.council.tenderapp.dlv.entities.Assignment;
import com.council.tenderapp.dlv.entities.AssignmentStatus;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@Service
@Transactional
public class AssignmentService {
    public static final List<AssignmentStatus> ACTIVE = List.of(AssignmentStatus.OFFERED, AssignmentStatus.ACCEPTED);
    private final AssignmentRepository assignments;
    private final CourierProfileRepository couriers;
    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final AssignmentAdoption adoption;

    public AssignmentService(AssignmentRepository assignments, CourierProfileRepository couriers, OrderRepository orders, OrderEventRepository events) {
        this.assignments = assignments;
        this.couriers = couriers;
        this.orders = orders;
        this.events = events;
        this.adoption = new AssignmentAdoption(assignments, couriers, events);
    }

    public List<CourierProfile> lockCouriers() { return couriers.findAllByOrderByUserIdAsc(); }

    public Optional<Assignment> current(Order order) {
        var active = assignments.findByOrderIdAndStatusIn(order.getId(), ACTIVE);
        if (active.isPresent()) {
            if (!active.get().getCourierId().equals(order.getCourierId())) {
                throw new InvalidOrderStateException("Order and assignment courier do not match");
            }
            adoption.synchronizeReservation(order);
            return active;
        }
        return adoption.adopt(order);
    }

    public Assignment accept(long courierId, long assignmentId) {
        var locked = lockCouriers();
        var assignment = assignments.findWithLockById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (!Long.valueOf(courierId).equals(assignment.getCourierId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This offer belongs to another courier");
        }
        var order = orders.findWithLockById(assignment.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        var courier = locked.stream().filter(c -> c.getUserId() == courierId).findFirst()
                .orElseThrow(() -> new InvalidOrderStateException("Courier is unavailable"));
        if (!assignment.getStatus().isActive() || order.getStatus() != OrderStatus.READY
                || !Long.valueOf(courierId).equals(order.getCourierId()) || courier.getStatus() != CourierStatus.ASSIGNED) {
            throw new InvalidOrderStateException("Offer is no longer available");
        }
        if (assignment.getStatus() == AssignmentStatus.OFFERED) {
            assignment.accept(Instant.now());
            events.save(new OrderEvent(order.getId(), "ready", "ready", "courier_accepted", assignment.getAcceptedAt()));
        }
        return assignment;
    }

    public boolean courierHasAssignment(long courierId) { return assignments.existsByCourierIdAndStatusIn(courierId, ACTIVE); }

    public void offer(Order order, CourierProfile courier) {
        if (order.getStatus() != OrderStatus.READY || order.getCourierId() != null || courier.getStatus() != CourierStatus.IDLE
                || assignments.findByOrderIdAndStatusIn(order.getId(), ACTIVE).isPresent() || courierHasAssignment(courier.getUserId())) {
            throw new InvalidOrderStateException("Order or courier is already assigned");
        }
        assignments.saveAndFlush(new Assignment(order.getId(), courier.getUserId(), Instant.now()));
        order.setCourierId(courier.getUserId());
        courier.setStatus(CourierStatus.ASSIGNED);
    }

    public void pickUp(Order order) {
        Assignment assignment = current(order)
                .orElseThrow(() -> new InvalidOrderStateException("Order must have an accepted courier assignment before pickup"));
        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new InvalidOrderStateException("Courier must accept the offer before pickup");
        }
        CourierProfile courier = couriers.findWithLockByUserId(assignment.getCourierId()).orElseThrow();
        if (courier.getStatus() != CourierStatus.ASSIGNED) throw new InvalidOrderStateException("Courier is not assigned");
        courier.setStatus(CourierStatus.CARRYING);
    }

    public void finish(Order order, boolean delivered) {
        Long courierId = order.getCourierId();
        var active = assignments.findByOrderIdAndStatusIn(order.getId(), ACTIVE);
        if (active.isPresent()) {
            if (!active.get().getCourierId().equals(order.getCourierId())) {
                throw new InvalidOrderStateException("Order and assignment courier do not match");
            }
            if (delivered && active.get().getStatus() != AssignmentStatus.ACCEPTED) {
                throw new InvalidOrderStateException("Courier must accept the offer before delivery completion");
            }
            boolean unaccepted = active.get().getStatus() == AssignmentStatus.OFFERED;
            if (delivered) active.get().complete();
            else active.get().end();
            if (unaccepted) order.setCourierId(null);
            assignments.flush();
        }
        if (courierId != null) {
            couriers.findWithLockByUserId(courierId).ifPresent(courier -> {
                if (!courierHasAssignment(courier.getUserId()) && (courier.getStatus() == CourierStatus.ASSIGNED
                        || courier.getStatus() == CourierStatus.CARRYING)) courier.setStatus(CourierStatus.IDLE);
            });
        }
    }

    public void end(Order order) { current(order).ifPresent(assignment -> { assignment.end(); assignments.flush(); }); }
}
