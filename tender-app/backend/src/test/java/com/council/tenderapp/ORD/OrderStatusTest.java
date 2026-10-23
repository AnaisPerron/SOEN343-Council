package com.council.tenderapp.ORD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.council.tenderapp.ORD.entities.OrderStatus;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void allowsOnlyLifecycleTransitions() {
        assertThat(OrderStatus.PLACED.transitionTo(OrderStatus.ACCEPTED))
            .isEqualTo(OrderStatus.ACCEPTED);
        assertThat(OrderStatus.PLACED.transitionTo(OrderStatus.REJECTED))
            .isEqualTo(OrderStatus.REJECTED);
        assertThat(OrderStatus.PLACED.transitionTo(OrderStatus.CANCELLED))
            .isEqualTo(OrderStatus.CANCELLED);
        assertThat(OrderStatus.ACCEPTED.transitionTo(OrderStatus.PREPARING))
            .isEqualTo(OrderStatus.PREPARING);
        assertThat(OrderStatus.PREPARING.transitionTo(OrderStatus.READY))
            .isEqualTo(OrderStatus.READY);
        assertThat(OrderStatus.READY.transitionTo(OrderStatus.PICKED_UP))
            .isEqualTo(OrderStatus.PICKED_UP);
        assertThat(OrderStatus.PICKED_UP.transitionTo(OrderStatus.DELIVERED))
            .isEqualTo(OrderStatus.DELIVERED);
        assertThat(OrderStatus.READY.transitionTo(OrderStatus.REFUNDED))
            .isEqualTo(OrderStatus.REFUNDED);
        assertThat(OrderStatus.PICKED_UP.transitionTo(OrderStatus.REFUNDED))
            .isEqualTo(OrderStatus.REFUNDED);
        assertThat(OrderStatus.DELIVERED.transitionTo(OrderStatus.REFUNDED))
            .isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void rejectsTransitionsThatSkipOrRepeatLifecycleStates() {
        assertThatThrownBy(() -> OrderStatus.PLACED.transitionTo(OrderStatus.PREPARING))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OrderStatus.ACCEPTED.transitionTo(OrderStatus.READY))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OrderStatus.READY.transitionTo(OrderStatus.PREPARING))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OrderStatus.REJECTED.transitionTo(OrderStatus.CANCELLED))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OrderStatus.READY.transitionTo(OrderStatus.DELIVERED))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OrderStatus.REFUNDED.transitionTo(OrderStatus.PICKED_UP))
            .isInstanceOf(IllegalStateException.class);
    }
}
