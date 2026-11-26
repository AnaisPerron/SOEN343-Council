package com.council.tenderapp.rvw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.council.tenderapp.operator.services.OperatorResetService;
import com.jayway.jsonpath.JsonPath;

/**
 * RVW demo script from the Reset state, on the real seed and the real PostgreSQL schema:
 * a rating accepted, a second rating of the same order refused, refusals for another
 * customer's order and an undelivered order, and the average shown to customers.
 *
 * Opt in with a disposable local PostgreSQL database, as for OperatorResetSeedIntegrationTest:
 * RESET_TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/reset_test (plus _USER and _PASSWORD).
 * Each run uses its own schema and drops it afterwards; Supabase is never used.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RESET_TEST_DATABASE_URL",
        matches = "jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/[^?]+")
class RatingResetAcceptanceTest {
    private static final String SCHEMA = "rvw_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String PASSWORD = "DemoPass123!"; // demo password from demo/seed.sql
    // Seed ids: Alice (1) owns delivered order 7 at Green Bowl Kitchen (2, paused) carried by Nora (7);
    // order 1 is Alice's order that is still placed; Omar (2) is another customer.
    private static final long DELIVERED_ORDER = 7;
    private static final long PLACED_ORDER = 1;
    private static final long GREEN_BOWL = 2;

    @Autowired private MockMvc mockMvc;
    @Autowired private OperatorResetService reset;
    @Autowired private JdbcTemplate jdbc;

    private static String url() { return System.getenv("RESET_TEST_DATABASE_URL"); }
    private static String user() { return System.getenv().getOrDefault("RESET_TEST_DATABASE_USER", "tender_reset_test"); }
    private static String password() { return System.getenv().getOrDefault("RESET_TEST_DATABASE_PASSWORD", ""); }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) throws Exception {
        String ddl = Files.readString(Path.of("docs/schema.txt"))
                .replace("SET transaction_timeout = 0;", "") // PostgreSQL 17 setting; allow local 16.
                .replace("public.", SCHEMA + ".").replace("SCHEMA public", "SCHEMA " + SCHEMA);
        try (var connection = DriverManager.getConnection(url(), user(), password());
                var statement = connection.createStatement()) {
            statement.execute(ddl); // the dump creates the schema itself
        }
        registry.add("spring.datasource.url", () -> url() + "?currentSchema=" + SCHEMA);
        registry.add("spring.datasource.username", RatingResetAcceptanceTest::user);
        registry.add("spring.datasource.password", RatingResetAcceptanceTest::password);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none"); // the schema comes from docs/schema.txt
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("app.operator.reset-enabled", () -> "true");
    }

    @AfterAll
    static void dropSchema() throws Exception {
        try (var connection = DriverManager.getConnection(url(), user(), password());
                var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @BeforeEach
    void startFromReset() {
        reset.reset();
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions rate(String bearer, long orderId, String body) throws Exception {
        return mockMvc.perform(post("/api/orders/{orderId}/rating", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long ratingRows() {
        return jdbc.queryForObject("SELECT count(*) FROM order_ratings", Long.class);
    }

    @Test
    void demoScriptAcceptRefuseAndShowAverage() throws Exception {
        String alice = login("alice@demo.test");
        String omar = login("omar@demo.test");
        assertThat(ratingRows()).isZero();

        // Refused: another customer's order, and Alice's order that is not delivered.
        rate(omar, DELIVERED_ORDER, "{\"restaurantStars\":1,\"courierStars\":1}")
                .andExpect(status().isForbidden());
        rate(alice, PLACED_ORDER, "{\"restaurantStars\":5}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order 1 cannot be rated because it is placed"));
        assertThat(ratingRows()).isZero();

        // Accepted.
        rate(alice, DELIVERED_ORDER, "{\"restaurantStars\":4,\"courierStars\":5,\"comment\":\"Still warm\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantId").value((int) GREEN_BOWL))
                .andExpect(jsonPath("$.courierId").value(7))
                .andExpect(jsonPath("$.restaurantStars").value(4))
                .andExpect(jsonPath("$.courierStars").value(5));

        // Second rating of the same order refused, first one kept.
        rate(alice, DELIVERED_ORDER, "{\"restaurantStars\":1,\"courierStars\":1}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order 7 has already been rated"));
        assertThat(ratingRows()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT restaurant_stars FROM order_ratings", Integer.class)).isEqualTo(4);

        // Green Bowl Kitchen is paused in the seed; the operator reopens it so customers see the average.
        mockMvc.perform(get("/api/restaurant/open").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 2)]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == 1)].averageRating").value(org.hamcrest.Matchers.contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id == 1)].ratingCount").value(org.hamcrest.Matchers.contains(0)));
        String operator = login("operator@demo.test");
        mockMvc.perform(put("/api/operator/restaurants/{id}/status", GREEN_BOWL)
                        .header(HttpHeaders.AUTHORIZATION, operator)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"open\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/restaurant/open").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 2)].averageRating").value(org.hamcrest.Matchers.contains(4.0)))
                .andExpect(jsonPath("$[?(@.id == 2)].ratingCount").value(org.hamcrest.Matchers.contains(1)));
    }

    @Test
    void resetClearsRatingsSoTheDemoCanBeReplayed() throws Exception {
        rate(login("alice@demo.test"), DELIVERED_ORDER, "{\"restaurantStars\":5,\"courierStars\":5}")
                .andExpect(status().isCreated());
        assertThat(ratingRows()).isEqualTo(1);

        reset.reset();

        assertThat(ratingRows()).isZero();
        rate(login("alice@demo.test"), DELIVERED_ORDER, "{\"restaurantStars\":3,\"courierStars\":3}")
                .andExpect(status().isCreated());
    }
}
