package com.council.tenderapp.ord.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.ord.commands.OrderCommand;
import com.council.tenderapp.ord.commands.OrderCommandInvoker;
import com.council.tenderapp.ord.observers.DashboardObserver;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

@WebMvcTest(controllers = RestaurantOrderController.class)
@Import({SecurityConfig.class, SessionService.class})
class RestaurantOrderControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private SessionService sessionService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private OrderCommandInvoker commandInvoker;
    @MockitoBean private DashboardObserver dashboardObserver;

    private String bearer(Long restaurantId, Role role) {
        return "Bearer " + sessionService.create(7L, restaurantId, role).token();
    }

    @Test
    void owningStaffCanReadIncomingOrders() throws Exception {
        when(orderService.getIncomingOrders(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/restaurants/1/orders/incoming")
                .header(HttpHeaders.AUTHORIZATION, bearer(1L, Role.STAFF)))
                .andExpect(status().isOk());
        verify(orderService).getIncomingOrders(1L);
    }

    @Test
    void restaurantActionsAreDispatchedThroughTheCommandInvoker() throws Exception {
        when(commandInvoker.execute(any(OrderCommand.class)))
                .thenAnswer(invocation -> ((OrderCommand) invocation.getArgument(0)).execute(orderService));

        mockMvc.perform(post("/api/restaurants/1/orders/42/accept")
                .header(HttpHeaders.AUTHORIZATION, bearer(1L, Role.STAFF)))
                .andExpect(status().isOk());

        verify(commandInvoker).execute(any(OrderCommand.class));
        verify(orderService).acceptOrder(1L, 42L);
    }

    @Test
    void invalidTransitionExplainsWhyItWasRefused() throws Exception {
        when(commandInvoker.execute(any(OrderCommand.class))).thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid order transition for action 'accept' from status ACCEPTED"));

        mockMvc.perform(post("/api/restaurants/1/orders/42/accept")
                .header(HttpHeaders.AUTHORIZATION, bearer(1L, Role.STAFF)))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Invalid order transition for action 'accept' from status ACCEPTED"));
    }

    @Test
    void otherRestaurantStaffAndCustomersCannotReadOrders() throws Exception {
        mockMvc.perform(get("/api/restaurants/1/orders/incoming")
                .header(HttpHeaders.AUTHORIZATION, bearer(2L, Role.STAFF)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/restaurants/1/orders/incoming")
                .header(HttpHeaders.AUTHORIZATION, bearer(1L, Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderService);
    }

    @Test
    void operatorsCannotPrepareOrders() throws Exception {
        for (String action : List.of("prepare", "ready")) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/restaurants/1/orders/42/" + action)
                    .header(HttpHeaders.AUTHORIZATION, bearer(1L, Role.OPERATOR)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(orderService);
    }

    @Test
    void restaurantRoutesCannotChangeCourierStateOrRefund() throws Exception {
        for (String action : List.of("picked-up", "delivered", "refund")) {
            mockMvc.perform(post("/api/restaurants/1/orders/42/" + action)
                    .header(HttpHeaders.AUTHORIZATION, bearer(1L, Role.STAFF)))
                    .andExpect(status().isNotFound());
        }
        verifyNoInteractions(orderService, commandInvoker);
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/restaurants/1/orders/incoming"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }
}
