package com.council.tenderapp.reg.security;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.repositories.RestaurantStaffRepository;
import com.council.tenderapp.reg.controllers.AuthController;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;
import com.council.tenderapp.reg.services.SessionService;
import com.jayway.jsonpath.JsonPath;

@WebMvcTest(controllers = {AuthController.class, OperatorSecurityTest.OperatorTestEndpoints.class})
@Import({SecurityConfig.class, LoginService.class, RegistrationService.class,
        SessionService.class, OperatorSecurityTest.OperatorTestEndpoints.class})
class OperatorSecurityTest {
    @RestController
    static class OperatorTestEndpoints {
        @GetMapping("/api/operator/test")
        Map<String, Boolean> read() { return Map.of("allowed", true); }

        @PostMapping("/api/operator/test")
        void create() {}

        @PutMapping("/api/operator/test")
        void update() {}
    }

    @Autowired private MockMvc mvc;
    @Autowired private SessionService sessions;
    @MockitoBean private ProfileRepository profiles;
    @MockitoBean private RestaurantRepository restaurants;
    @MockitoBean private RestaurantStaffRepository staff;

    private String bearer(Role role) {
        return "Bearer " + sessions.create(8L, role).token();
    }

    @Test
    void anonymousRequestsAreUnauthorized() throws Exception {
        mvc.perform(get("/api/operator/test")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/operator/test")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/operator/test")).andExpect(status().isUnauthorized());
    }

    @Test
    void otherRolesCannotReadOrWriteEvenWhenClaimingToBeOperators() throws Exception {
        for (Role role : List.of(Role.CUSTOMER, Role.COURIER, Role.STAFF)) {
            String token = bearer(role);
            mvc.perform(get("/api/operator/test?role=OPERATOR").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"OPERATOR\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void operatorsCanReadAndWrite() throws Exception {
        String token = bearer(Role.OPERATOR);
        mvc.perform(get("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(true));
        mvc.perform(post("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mvc.perform(put("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void unknownAndLoggedOutTokensAreUnauthorized() throws Exception {
        mvc.perform(get("/api/operator/test").header(HttpHeaders.AUTHORIZATION, "Bearer unknown"))
                .andExpect(status().isUnauthorized());
        String token = bearer(Role.OPERATOR);
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredOperatorSessionsAreUnauthorized() throws Exception {
        Instant now = Instant.parse("2026-10-29T12:00:00Z");
        try {
            ReflectionTestUtils.setField(sessions, "clock", Clock.fixed(now, ZoneOffset.UTC));
            String token = bearer(Role.OPERATOR);
            ReflectionTestUtils.setField(sessions, "clock", Clock.fixed(now.plusSeconds(172800), ZoneOffset.UTC));
            mvc.perform(get("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnauthorized());
        } finally {
            ReflectionTestUtils.setField(sessions, "clock", Clock.systemUTC());
        }
    }

    @Test
    void predefinedOperatorCanLogInRestoreAndUseItsSession() throws Exception {
        Profile profile = new Profile(Role.OPERATOR, "Olivia Operator", "operator@demo.test",
                "$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi", "+15145550108");
        ReflectionTestUtils.setField(profile, "id", 8L);
        when(profiles.findByEmailIgnoreCase("operator@demo.test")).thenReturn(Optional.of(profile));
        when(profiles.findById(8L)).thenReturn(Optional.of(profile));
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"operator@demo.test\",\"password\":\"DemoPass123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(response, "$.token");
        mvc.perform(get("/api/auth/session").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Olivia Operator"));
        mvc.perform(get("/api/operator/test").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void publicRegistrationCannotCreateOperatorAccounts() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"New Operator","email":"new@example.test","password":"password123",
                 "phone":"5145550100","role":"OPERATOR"}
                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(profiles, restaurants, staff);
    }
}
