package com.council.tenderapp.dlv.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
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

    public AssignmentService(AssignmentRepository assignments, CourierProfileRepository couriers) {
        this.assignments = assignments;
        this.couriers = couriers;
    }

    public List<CourierProfile> lockCouriers() { return couriers.findAllByOrderByUserIdAsc(); }

    public Optional<Assignment> current(Order order) {
        var active = assignments.findByOrderIdAndStatusIn(order.getId(), ACTIVE);
        if (active.isPresent()) {
            if (!active.get().getCourierId().equals(order.getCourierId())) {
                throw new InvalidOrderStateException("Order and assignment courier do not match");
            }
            return active;
        }
        if (order.getCourierId() == null || (order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.PICKED_UP)) {
            return Optional.empty();
        }
        if (assignments.existsByCourierIdAndStatusIn(order.getCourierId(), ACTIVE)) {
            throw new InvalidOrderStateException("Courier already has another active assignment");
        }
        Instant now = Instant.now();
        Assignment adopted = new Assignment(order.getId(), order.getCourierId(), now);
        if (order.getStatus() == OrderStatus.PICKED_UP) adopted.accept(now);
        return Optional.of(assignments.saveAndFlush(adopted));
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
        var active = assignments.findByOrderIdAndStatusIn(order.getId(), ACTIVE);
        if (active.isPresent()) {
            if (!active.get().getCourierId().equals(order.getCourierId())) {
                throw new InvalidOrderStateException("Order and assignment courier do not match");
            }
            if (delivered && active.get().getStatus() != AssignmentStatus.ACCEPTED) {
                throw new InvalidOrderStateException("Courier must accept the offer before delivery completion");
            }
            if (delivered) active.get().complete();
            else active.get().end();
            assignments.flush();
        }
        if (order.getCourierId() != null) {
            couriers.findWithLockByUserId(order.getCourierId()).ifPresent(courier -> {
                if (!courierHasAssignment(courier.getUserId()) && (courier.getStatus() == CourierStatus.ASSIGNED
                        || courier.getStatus() == CourierStatus.CARRYING)) courier.setStatus(CourierStatus.IDLE);
            });
        }
    }

    public void end(Order order) { current(order).ifPresent(assignment -> { assignment.end(); assignments.flush(); }); }
}
