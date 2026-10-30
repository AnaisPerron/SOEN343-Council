package com.council.tenderapp.ord.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/restaurants/1/orders/incoming"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }
}
