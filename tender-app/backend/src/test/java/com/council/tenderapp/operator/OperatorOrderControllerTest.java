package com.council.tenderapp.operator;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.council.tenderapp.operator.controllers.OperatorOrderController;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.services.OrderCancellationService;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

@WebMvcTest(controllers = OperatorOrderController.class)
@Import({SecurityConfig.class, SessionService.class})
class OperatorOrderControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private SessionService sessions;
    @MockitoBean private OrderCancellationService cancellations;

    private String bearer(Role role) { return "Bearer " + sessions.create(8L, role).token(); }

    @Test
    void operatorCanCancelWithoutSupplyingACustomerId() throws Exception {
        mvc.perform(post("/api/operator/orders/42/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isNoContent());
        verify(cancellations).cancelForOperator(42L);
    }

    @Test
    void missingAndInvalidSessionsAreUnauthorized() throws Exception {
        mvc.perform(post("/api/operator/orders/42/cancel")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/operator/orders/42/cancel")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cancellations);
    }

    @Test
    void customerCourierAndStaffCannotUseTheOperatorEndpoint() throws Exception {
        for (Role role : List.of(Role.CUSTOMER, Role.COURIER, Role.STAFF)) {
            mvc.perform(post("/api/operator/orders/42/cancel")
                    .header(HttpHeaders.AUTHORIZATION, bearer(role)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(cancellations);
    }

    @Test
    void invalidIdsDoNotReachTheService() throws Exception {
        for (String id : List.of("0", "-1", "invalid")) {
            mvc.perform(post("/api/operator/orders/" + id + "/cancel")
                    .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(cancellations);
    }

    @Test
    void missingOrderReturnsNotFound() throws Exception {
        doThrow(new OrderNotFoundException("Order not found with id: 42"))
                .when(cancellations).cancelForOperator(42L);
        mvc.perform(post("/api/operator/orders/42/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 42"));
    }

    @Test
    void invalidOrderStateReturnsConflict() throws Exception {
        doThrow(new InvalidOrderStateException("Order cannot be cancelled"))
                .when(cancellations).cancelForOperator(42L);
        mvc.perform(post("/api/operator/orders/42/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR)))
                .andExpect(status().isConflict());
    }
}
