package com.council.tenderapp.operator;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.council.tenderapp.operator.controllers.OperatorRestaurantController;
import com.council.tenderapp.operator.dto.RestaurantStatus;
import com.council.tenderapp.operator.services.OperatorRestaurantService;
import com.council.tenderapp.ord.exceptions.RestaurantNotFoundException;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

@WebMvcTest(controllers = OperatorRestaurantController.class)
@Import({SecurityConfig.class, SessionService.class})
class OperatorRestaurantControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private SessionService sessions;
    @MockitoBean private OperatorRestaurantService service;

    private String bearer(Role role) { return "Bearer " + sessions.create(8L, role).token(); }

    @Test
    void operatorCanSetEverySupportedStatus() throws Exception {
        for (RestaurantStatus state : RestaurantStatus.values()) {
            mvc.perform(put("/api/operator/restaurants/1/status")
                    .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + state.value() + "\"}"))
                    .andExpect(status().isNoContent());
            verify(service).updateStatus(1L, state);
        }
    }

    @Test
    void missingAndInvalidSessionsAreUnauthorized() throws Exception {
        mvc.perform(put("/api/operator/restaurants/1/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"paused\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/operator/restaurants/1/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"paused\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void otherRolesCannotChangeAnyRestaurant() throws Exception {
        for (Role role : List.of(Role.CUSTOMER, Role.COURIER, Role.STAFF)) {
            mvc.perform(put("/api/operator/restaurants/1/status")
                    .header(HttpHeaders.AUTHORIZATION, bearer(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"open\"}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingOrInvalidStatusesAreRejectedBeforeServiceCalls() throws Exception {
        for (String body : List.of("{}", "{\"status\":null}", "{\"status\":\"\"}",
                "{\"status\":\"busy\"}", "{\"status\":\"OPEN\"}", "{\"status\":0}",
                "{\"status\":true}", "{", "null")) {
            mvc.perform(put("/api/operator/restaurants/1/status")
                    .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void invalidRestaurantIdsAreRejectedBeforeServiceCalls() throws Exception {
        for (String id : List.of("0", "-1", "invalid")) {
            mvc.perform(put("/api/operator/restaurants/" + id + "/status")
                    .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"paused\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void unknownRestaurantReturnsNotFound() throws Exception {
        doThrow(new RestaurantNotFoundException("Restaurant not found with id: 999"))
                .when(service).updateStatus(999L, RestaurantStatus.PAUSED);
        mvc.perform(put("/api/operator/restaurants/999/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"paused\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Restaurant not found with id: 999"));
    }
}
