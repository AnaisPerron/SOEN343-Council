package com.council.tenderapp.operator;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.operator.controllers.OperatorDashboardController;
import com.council.tenderapp.operator.dto.CourierSummary;
import com.council.tenderapp.operator.dto.CustomerSummary;
import com.council.tenderapp.operator.dto.DashboardPage;
import com.council.tenderapp.operator.dto.DashboardSummary;
import com.council.tenderapp.operator.dto.RestaurantSummary;
import com.council.tenderapp.operator.services.OperatorDashboardService;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.dto.response.OrderResponse;

@WebMvcTest(controllers = OperatorDashboardController.class)
@Import({SecurityConfig.class, SessionService.class})
class OperatorDashboardControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private SessionService sessions;
    @MockitoBean private OperatorDashboardService service;
    @MockitoBean private OrderService orderService;
    @MockitoBean private OrderRepository orderRepository;

    private String bearer(Role role) { return "Bearer " + sessions.create(8L, role).token(); }

    @Test
    void allDashboardRoutesRequireOperatorSessions() throws Exception {
        for (String route : List.of("summary", "restaurants", "orders", "customers", "couriers")) {
            mvc.perform(get("/api/operator/" + route)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/operator/" + route).header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                    .andExpect(status().isUnauthorized());
            for (Role role : List.of(Role.CUSTOMER, Role.COURIER, Role.STAFF)) {
                mvc.perform(get("/api/operator/" + route).header(HttpHeaders.AUTHORIZATION, bearer(role)))
                        .andExpect(status().isForbidden());
            }
        }
        verifyNoInteractions(service);
    }

    @Test
    void operatorReceivesSummaryCounts() throws Exception {
        when(service.summary()).thenReturn(new DashboardSummary(4, 8, 3, 2, 1));
        mvc.perform(get("/api/operator/summary").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.restaurantCount").value(4))
                .andExpect(jsonPath("$.availableCourierCount").value(1));
    }

    @Test
    void listDefaultsAndPaginationArePassedToTheService() throws Exception {
        when(service.restaurants(0, 50)).thenReturn(new DashboardPage<>(List.of(
                new RestaurantSummary(1, "Maple & Main", "open", null, null, null, null, 0)), 4, 0, 50));
        when(service.orders(1, 2)).thenReturn(new DashboardPage<>(List.of(), 8, 1, 2));
        mvc.perform(get("/api/operator/restaurants").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("Maple & Main"))
                .andExpect(jsonPath("$.total").value(4)).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
        mvc.perform(get("/api/operator/orders?page=1&size=2").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(8)).andExpect(jsonPath("$.page").value(1));
        verify(service).restaurants(0, 50);
        verify(service).orders(1, 2);
    }

    @Test
    void orderFiltersReachTheServiceAndInvalidDatesAreRejected() throws Exception {
        when(service.orders(0, 50, List.of("delivered", "refunded"), List.of(10L),
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-11"), ZoneId.of("America/Toronto")))
                .thenReturn(new DashboardPage<>(List.of(), 0, 0, 50));
        mvc.perform(get("/api/operator/orders?statuses=delivered&statuses=refunded&restaurantIds=10&from=2026-11-01&to=2026-11-11&timeZone=America/Toronto")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        verify(service).orders(0, 50, List.of("delivered", "refunded"), List.of(10L),
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-11"), ZoneId.of("America/Toronto"));
        mvc.perform(get("/api/operator/orders?from=2026-11-12&to=2026-11-11")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))).andExpect(status().isBadRequest());
    }

    @Test
    void refundButtonEndpointInvokesOrderRefundOnlyForOperator() throws Exception {
        Order order = mock(Order.class);
        when(order.getRestaurantId()).thenReturn(10L);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderService.refundOrder(10L, 20L)).thenReturn(new OrderResponse(20L, 10L,
                OrderStatus.REFUNDED, null, null, null, new BigDecimal("12.50"), null, null, null, List.of()));
        mvc.perform(post("/api/operator/orders/20/refund").header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/operator/orders/20/refund").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUNDED"));
        verify(orderService).refundOrder(10L, 20L);
    }

    @Test
    void contactDtosExposeNoPasswordOrTokenFields() throws Exception {
        when(service.customers(0, 50)).thenReturn(new DashboardPage<>(List.of(
                new CustomerSummary(1, "Alice", "alice@example.test", null, new BigDecimal("25.00"), null, null)), 1, 0, 50));
        when(service.couriers(0, 50)).thenReturn(new DashboardPage<>(List.of(
                new CourierSummary(2, "Courier", "courier@example.test", null, null, null, null)), 1, 0, 50));
        for (String route : List.of("customers", "couriers")) {
            mvc.perform(get("/api/operator/" + route).header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").isNumber())
                    .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.items[0].password_hash").doesNotExist())
                    .andExpect(jsonPath("$.items[0].token").doesNotExist());
        }
    }

    @Test
    void invalidPaginationIsRejectedBeforeServiceCalls() throws Exception {
        for (String route : List.of("restaurants", "orders", "customers", "couriers")) {
            for (String query : List.of("page=-1", "size=0", "size=101", "page=abc", "size=-1")) {
                mvc.perform(get("/api/operator/" + route + "?" + query)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                        .andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(service);
    }
}
