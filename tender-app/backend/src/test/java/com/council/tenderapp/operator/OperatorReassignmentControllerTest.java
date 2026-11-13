package com.council.tenderapp.operator;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.council.tenderapp.operator.controllers.OperatorReassignmentController;
import com.council.tenderapp.operator.services.OperatorReassignmentService;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

@WebMvcTest(controllers = OperatorReassignmentController.class)
@Import({SecurityConfig.class, SessionService.class})
class OperatorReassignmentControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private SessionService sessions;
    @MockitoBean private OperatorReassignmentService service;

    private String bearer(Role role) { return "Bearer " + sessions.create(8L, role).token(); }

    @Test
    void operatorsCanReassign() throws Exception {
        mvc.perform(post("/api/operator/orders/1/reassign")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedCourierId\":101}"))
                .andExpect(status().isNoContent());
        verify(service).reassign(1L, 101L);
    }

    @Test
    void anonymousInvalidAndExpiredTokensAreUnauthorized() throws Exception {
        String expired = sessions.create(8L, Role.OPERATOR).token();
        sessions.invalidate(expired);
        for (String authorization : List.of("", "Bearer invalid", "Bearer " + expired)) {
            mvc.perform(post("/api/operator/orders/1/reassign").header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"expectedCourierId\":101}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }

    @Test
    void nonOperatorsAreForbidden() throws Exception {
        for (Role role : List.of(Role.CUSTOMER, Role.COURIER, Role.STAFF)) {
            mvc.perform(post("/api/operator/orders/1/reassign").header(HttpHeaders.AUTHORIZATION, bearer(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"expectedCourierId\":101}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void invalidBodiesAndIdsAreBadRequests() throws Exception {
        for (String body : List.of("{}", "null", "{", "{\"expectedCourierId\":null}",
                "{\"expectedCourierId\":0}", "{\"expectedCourierId\":-1}", "{\"expectedCourierId\":\"invalid\"}")) {
            mvc.perform(post("/api/operator/orders/1/reassign")
                    .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        for (String id : List.of("0", "-1", "invalid")) {
            mvc.perform(post("/api/operator/orders/" + id + "/reassign")
                    .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"expectedCourierId\":101}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingOrdersAreNotFoundAndStaleOrIneligibleAssignmentsConflict() throws Exception {
        doThrow(new OrderNotFoundException("Order not found")).when(service).reassign(999L, 101L);
        doThrow(new InvalidOrderStateException("Assignment changed")).when(service).reassign(1L, 101L);
        mvc.perform(post("/api/operator/orders/999/reassign")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedCourierId\":101}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/operator/orders/1/reassign")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedCourierId\":101}"))
                .andExpect(status().isConflict());
    }
}
