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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.operator.controllers.OperatorResetController;
import com.council.tenderapp.operator.services.OperatorResetService;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

@WebMvcTest(controllers = OperatorResetController.class)
@Import({SecurityConfig.class, SessionService.class})
class OperatorResetControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private SessionService sessions;
    @MockitoBean private OperatorResetService service;

    private String bearer(Role role) { return "Bearer " + sessions.create(8L, role).token(); }

    @Test
    void operatorCanRequestResetWithExplicitConfirmation() throws Exception {
        mvc.perform(post("/api/operator/reset").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"RESET\"}"))
                .andExpect(status().isNoContent());
        verify(service).reset();
    }

    @Test
    void anonymousAndInvalidTokensAreUnauthorized() throws Exception {
        for (String authorization : List.of("", "Bearer invalid")) {
            mvc.perform(post("/api/operator/reset").header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"RESET\"}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }

    @Test
    void otherRolesAreForbidden() throws Exception {
        for (Role role : List.of(Role.CUSTOMER, Role.COURIER, Role.STAFF)) {
            mvc.perform(post("/api/operator/reset").header(HttpHeaders.AUTHORIZATION, bearer(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"RESET\"}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void confirmationIsRequiredAndMustMatchExactly() throws Exception {
        for (String body : List.of("{}", "null", "{", "{\"confirmation\":null}",
                "{\"confirmation\":\"reset\"}", "{\"confirmation\":\"\"}", "{\"confirmation\":true}")) {
            mvc.perform(post("/api/operator/reset").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void disabledResetReturnsConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "System reset is disabled")).when(service).reset();
        mvc.perform(post("/api/operator/reset").header(HttpHeaders.AUTHORIZATION, bearer(Role.OPERATOR))
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"RESET\"}"))
                .andExpect(status().isConflict());
    }
}
