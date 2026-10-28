package com.council.tenderapp.reg.security;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.config.SecurityConfig;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.controllers.AuthController;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;
import com.council.tenderapp.reg.services.SessionService;
import com.jayway.jsonpath.JsonPath;

// Runs the real security filter chain, login and session services; only the database is mocked
@WebMvcTest(controllers = {AuthController.class, SessionGateTest.ProtectedEndpoints.class})
@Import({SecurityConfig.class, LoginService.class, SessionService.class, SessionGateTest.ProtectedEndpoints.class})
class SessionGateTest {

    private static final String EMAIL = "jane@example.com";
    private static final String PASSWORD = "secret123";
    private static final String MENU_PATTERN = "/api/restaurants/{restaurantId}/menu";
    private static final String MENU = "/api/restaurants/1/menu";
    private static final String PLACE_ORDER = "/api/orders";

    // Stand-ins for protected API endpoints, which are not on the backend yet.
    @RestController
    static class ProtectedEndpoints {

        @GetMapping(MENU_PATTERN)
        Map<String, Object> menu(@PathVariable Long restaurantId) {
            return Map.of("restaurantId", restaurantId, "items", List.of());
        }

        @PostMapping(PLACE_ORDER)
        Map<String, String> placeOrder() {
            return Map.of("status", "PLACED");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ProfileRepository repository;

    @MockitoBean
    private RestaurantRepository restaurantRepository;

    @MockitoBean
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        Profile profile = new Profile(Role.CUSTOMER, "Jane", EMAIL, passwordEncoder.encode(PASSWORD), "514-555-0101");
        ReflectionTestUtils.setField(profile, "id", 7L);
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(profile));
    }

    private String loginBody(String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(EMAIL, password);
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void allSeededMenuPhotosAreServedWithoutSession() throws Exception {
        for (String filename : List.of(
                "maple-glazed-chicken-sandwich.jpg", "truffle.jpg", "apple-crumble.jpg",
                "harvest-grain-bowl.jpg", "miso-tofu-bowl.jpg", "smoked-salmon-bagel.jpg",
                "margherita-pizza.jpg")) {
            byte[] expected = Files.readAllBytes(Path.of("../frontend/src/mocks/menu-items-imgs", filename));
            mockMvc.perform(get("/menu-items-imgs/" + filename))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
                    .andExpect(content().bytes(expected));
        }
    }

    @Test
    void validLoginReturnsSession() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.expiresAt").value(notNullValue()))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody("wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void protectedCommandWithoutSessionIsRefused() throws Exception {
        mockMvc.perform(post(PLACE_ORDER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedCommandWithUnknownTokenIsRefused() throws Exception {
        mockMvc.perform(post(PLACE_ORDER).header(HttpHeaders.AUTHORIZATION, bearer("not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void browsingMenuWithoutSessionIsRefused() throws Exception {
        mockMvc.perform(get(MENU))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validSessionPassesTheGate() throws Exception {
        String token = login();

        mockMvc.perform(get(MENU).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post(PLACE_ORDER).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void oldSessionStopsWorkingAfterLogout() throws Exception {
        String token = login();
        mockMvc.perform(get(MENU).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(MENU).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PLACE_ORDER).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }
}
