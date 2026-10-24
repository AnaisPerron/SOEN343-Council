package com.council.tenderapp.ord.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.ord.dtos.OrderResponseDto;
import com.council.tenderapp.ord.dtos.OrderStatusResponseDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

// Runs the real security filter chain and session service; the order service is mocked
@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, SessionService.class})
class OrderControllerTest {

    private static final String PLACE_ORDER = "/api/orders";
    private static final String CANCEL_ORDER = "/api/orders/{id}/cancel";
    private static final String VALID_BODY = """
            {"restaurantId":1,"items":[{"menuItemId":1,"quantity":2}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @MockitoBean
    private OrderService orderService;

    private String bearer(Role role) {
        return "Bearer " + sessionService.create(7L, role).token();
    }

    @Test
    void customerCanPlaceOrder() throws Exception {
        when(orderService.placeOrder(eq(7L), any(PlaceOrderRequestDto.class))).thenReturn(
                new OrderResponseDto(42L, 1L, "placed", new BigDecimal("28.00"), Instant.now(), "MOCK-1", List.of()));

        mockMvc.perform(post(PLACE_ORDER).header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.status").value("placed"))
                .andExpect(jsonPath("$.total").value(28.00));
        verify(orderService).placeOrder(eq(7L), any(PlaceOrderRequestDto.class));
    }

    @Test
    void nonCustomerRolesAreForbidden() throws Exception {
        for (Role role : List.of(Role.COURIER, Role.STAFF, Role.OPERATOR)) {
            mockMvc.perform(post(PLACE_ORDER).header(HttpHeaders.AUTHORIZATION, bearer(role))
                            .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(orderService);
    }

    @Test
    void missingSessionIsUnauthorized() throws Exception {
        mockMvc.perform(post(PLACE_ORDER).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void zeroQuantityIsRejected() throws Exception {
        mockMvc.perform(post(PLACE_ORDER).header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restaurantId":1,"items":[{"menuItemId":1,"quantity":0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").value("Quantity must be greater than 0"));
        verifyNoInteractions(orderService);
    }

    @Test
    void emptyItemListIsRejected() throws Exception {
        mockMvc.perform(post(PLACE_ORDER).header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restaurantId":1,"items":[]}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void customerCanCancelOwnOrder() throws Exception {
        when(orderService.cancelOrder(7L, 42L)).thenReturn(new OrderStatusResponseDto(42L, "placed", "cancelled", Instant.now()));

        mockMvc.perform(post(CANCEL_ORDER, 42).header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.status").value("cancelled"));
    }

    @Test
    void cancellingAnotherCustomersOrderIsForbidden() throws Exception {
        when(orderService.cancelOrder(7L, 42L)).thenThrow(new OrderAccessDeniedException("Order 42 does not belong to this customer"));

        mockMvc.perform(post(CANCEL_ORDER, 42).header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancellingOrderPastPlacedIsConflict() throws Exception {
        when(orderService.cancelOrder(7L, 42L)).thenThrow(new InvalidOrderStateException("Order 42 cannot be cancelled because it is accepted"));

        mockMvc.perform(post(CANCEL_ORDER, 42).header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order 42 cannot be cancelled because it is accepted"));
    }

    @Test
    void cancellingUnknownOrderIsNotFound() throws Exception {
        when(orderService.cancelOrder(7L, 99L)).thenThrow(new OrderNotFoundException("Order not found with id: 99"));

        mockMvc.perform(post(CANCEL_ORDER, 99).header(HttpHeaders.AUTHORIZATION, bearer(Role.CUSTOMER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonCustomerRolesCannotCancel() throws Exception {
        for (Role role : List.of(Role.COURIER, Role.STAFF, Role.OPERATOR)) {
            mockMvc.perform(post(CANCEL_ORDER, 42).header(HttpHeaders.AUTHORIZATION, bearer(role)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(CANCEL_ORDER, 42))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }
}
