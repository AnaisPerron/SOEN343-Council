package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.dlv.entities.Assignment;
import com.council.tenderapp.dlv.entities.AssignmentStatus;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.dlv.services.AssignmentService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceCharacterizationTest {
    @Mock AssignmentRepository assignments;
    @Mock CourierProfileRepository couriers;
    @Mock OrderRepository orders;
    @Mock OrderEventRepository events;

    private AssignmentService service() {
        return new AssignmentService(assignments, couriers, orders, events);
    }

    private static Order order(OrderStatus status) {
        Order order = new Order(1L, new Date(1_000L), status);
        order.setId(10L);
        order.setCourierId(77L);
        return order;
    }

    @Test
    void currentReturnsMatchingActiveAssignmentAndReservesIdleCourier() {
        Order order = order(OrderStatus.READY);
        Assignment active = new Assignment(10L, 77L, Instant.parse("2026-01-01T00:00:00Z"));
        CourierProfile courier = new CourierProfile(77L);
        courier.setStatus(CourierStatus.IDLE);
        when(assignments.findByOrderIdAndStatusIn(10L, AssignmentService.ACTIVE)).thenReturn(Optional.of(active));
        when(couriers.findWithLockByUserId(77L)).thenReturn(Optional.of(courier));

        assertThat(service().current(order)).containsSame(active);

        assertThat(courier.getStatus()).isEqualTo(CourierStatus.ASSIGNED);
        verify(assignments).findByOrderIdAndStatusIn(eq(10L), eq(List.of(AssignmentStatus.OFFERED, AssignmentStatus.ACCEPTED)));
        verifyNoInteractions(events);
    }

    @Test
    void currentRejectsActiveAssignmentForDifferentCourierBeforeReservation() {
        Assignment active = new Assignment(10L, 88L, Instant.parse("2026-01-01T00:00:00Z"));
        when(assignments.findByOrderIdAndStatusIn(10L, AssignmentService.ACTIVE)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service().current(order(OrderStatus.READY)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order and assignment courier do not match");
        verifyNoInteractions(couriers, events);
    }

    @Test
    void currentReturnsEmptyForDeliveredOrderWithoutActiveAssignment() {
        assertThat(service().current(order(OrderStatus.DELIVERED))).isEmpty();

        verifyNoInteractions(couriers, events);
    }

    @Test
    void currentAdoptsReadyOrderAndPreservesAcceptedCourierEvent() {
        Order order = order(OrderStatus.READY);
        CourierProfile courier = new CourierProfile(77L);
        courier.setStatus(CourierStatus.ASSIGNED);
        when(couriers.findWithLockByUserId(77L)).thenReturn(Optional.of(courier));
        when(events.existsByOrderIdAndEventType(10L, "courier_accepted")).thenReturn(true);
        when(assignments.saveAndFlush(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Assignment adopted = service().current(order).orElseThrow();

        assertThat(adopted.getOrderId()).isEqualTo(10L);
        assertThat(adopted.getCourierId()).isEqualTo(77L);
        assertThat(adopted.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
        assertThat(adopted.getAcceptedAt()).isNotNull();
        verify(assignments).saveAndFlush(adopted);
    }
}
