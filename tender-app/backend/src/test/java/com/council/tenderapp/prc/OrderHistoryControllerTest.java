package com.council.tenderapp.prc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.prc.controllers.OrderHistoryController;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.security.Session;

class OrderHistoryControllerTest {
    @Test
    void filtersByTheAuthenticatedRoleAndIdentity() {
        OrderRepository orders = mock(OrderRepository.class);
        Order delivered = new Order(4L, 9L, OrderStatus.DELIVERED, new BigDecimal("14.10"));
        delivered.setId(12L);
        when(orders.findByCustomerIdOrderByDateOrderedDescIdDesc(4L)).thenReturn(List.of(delivered));
        when(orders.findByRestaurantIdOrderByDateOrderedDescIdDesc(9L)).thenReturn(List.of(delivered));
        when(orders.findByCourierIdAndStatusInOrderByDateOrderedDescIdDesc(6L,
                List.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED))).thenReturn(List.of(delivered));
        when(orders.findAll()).thenReturn(List.of(delivered));
        OrderHistoryController controller = new OrderHistoryController(orders);

        for (Role role : Role.values()) {
            var auth = new UsernamePasswordAuthenticationToken(4L, null);
            auth.setDetails(new Session("token", role == Role.COURIER ? 6L : 4L,
                    role == Role.STAFF ? 9L : null, role, Instant.now().plusSeconds(60)));
            var history = controller.history(auth);
            assertThat(history).hasSize(1);
            assertThat(history.get(0).orderId()).isEqualTo(12L);
            assertThat(history.get(0).totalCharge()).isEqualByComparingTo("14.10");
        }
    }
}
