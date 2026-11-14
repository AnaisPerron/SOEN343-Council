package com.council.tenderapp.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.council.tenderapp.operator.reset.ResetCoordinator;
import com.council.tenderapp.operator.reset.ResetSeedLoader;
import com.council.tenderapp.operator.services.OperatorResetService;
import com.council.tenderapp.ord.observers.DashboardObserver;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

// Opt in with a disposable local PostgreSQL database; Supabase is never used.
@EnabledIfEnvironmentVariable(named = "RESET_TEST_DATABASE_URL",
        matches = "jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/[^?]+")
class OperatorResetSeedIntegrationTest {
    private DriverManagerDataSource datasource;
    private JdbcTemplate jdbc;
    private String schema;
    private SessionService sessions;
    private DashboardObserver dashboards;

    @BeforeEach
    void setUp() throws Exception {
        schema = "reset_test_" + UUID.randomUUID().toString().replace("-", "");
        datasource = new DriverManagerDataSource(System.getenv("RESET_TEST_DATABASE_URL"),
                System.getenv().getOrDefault("RESET_TEST_DATABASE_USER", "tender_reset_test"),
                System.getenv().getOrDefault("RESET_TEST_DATABASE_PASSWORD", ""));
        var properties = new Properties();
        properties.setProperty("currentSchema", schema);
        datasource.setConnectionProperties(properties);
        jdbc = new JdbcTemplate(datasource);
        String ddl = Files.readString(Path.of("docs/schema.txt"))
                .replace("SET transaction_timeout = 0;", "") // PostgreSQL 17 setting; allow local 16.
                .replace("public.", schema + ".").replace("SCHEMA public", "SCHEMA " + schema);
        jdbc.execute(ddl);
        jdbc.update("INSERT INTO health_check (status) VALUES ('preserved')");
        sessions = new SessionService(Duration.ofHours(24));
        dashboards = mock(DashboardObserver.class);
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null) jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
    }

    private OperatorResetService service(Resource seed) {
        return new OperatorResetService(new ResetCoordinator(), new ResetSeedLoader(jdbc, seed),
                sessions, dashboards, new DataSourceTransactionManager(datasource), true);
    }

    @Test
    void actualSeedRestoresBaselineIncludingNewForeignKeyTablesAndResetsIdentities() {
        var reset = service(new ClassPathResource("demo/seed.sql"));
        reset.reset();
        jdbc.update("UPDATE menu_items SET price = 70 WHERE id = 3");
        jdbc.update("INSERT INTO tender_cash_transactions (customer_id, order_id, reason, tenders) VALUES (1, 1, 'earn', 10)");
        jdbc.update("INSERT INTO assignments (order_id, courier_id) VALUES (5, 6)");
        jdbc.update("INSERT INTO menu_photos (restaurant_id, content_type, data) VALUES (1, 'image/jpeg', decode('00', 'hex'))");
        String token = sessions.create(8L, Role.OPERATOR).token();

        reset.reset();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bill", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ledger_entries", Integer.class)).isEqualTo(8);
        for (String table : new String[]{"assignments", "menu_photos", "tender_cash_transactions", "auth_sessions"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class)).as(table).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT price FROM menu_items WHERE id = 3", java.math.BigDecimal.class))
                .isEqualByComparingTo("7.00");
        assertThat(jdbc.queryForObject("SELECT total_price FROM orders WHERE id = 1", java.math.BigDecimal.class))
                .isEqualByComparingTo("22.75");
        assertThat(jdbc.queryForObject("SELECT total_price FROM orders WHERE id = 7", java.math.BigDecimal.class))
                .isEqualByComparingTo("16.50");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM order_lines WHERE item_snapshot IS NULL", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders WHERE delivery_address IS NULL", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM health_check", String.class)).isEqualTo("preserved");
        assertThat(jdbc.queryForObject("SELECT max(id) FROM bill", Long.class)).isEqualTo(8L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM restaurants WHERE cover_url IS NOT NULL", Integer.class)).isEqualTo(4);
        for (String url : jdbc.queryForList("SELECT photo_url FROM menu_items UNION SELECT cover_url FROM restaurants", String.class)) {
            assertThat(url).startsWith("/menu-items-imgs/");
            assertThat(new ClassPathResource("static" + url).exists()).as(url).isTrue();
        }
        assertThat(sessions.find(token)).isEmpty();
    }

    @Test
    void imageRepairRestoresMissingCoversAndPreservesCustomUrls() throws Exception {
        service(new ClassPathResource("demo/seed.sql")).reset();
        jdbc.update("UPDATE restaurants SET cover_url = 'https://cdn.example.test/custom-cover.jpg' WHERE id = 1");
        jdbc.update("UPDATE restaurants SET cover_url = NULL WHERE id = 2");
        jdbc.update("UPDATE menu_items SET photo_url = 'http://localhost:8080/menu-items-imgs/maple-glazed-chicken-sandwich.jpg' WHERE id = 1");
        jdbc.update("UPDATE menu_items SET photo_url = 'https://cdn.example.test/custom-menu.jpg' WHERE id = 2");
        String repair = Files.readString(Path.of("docs/migrations/2026-11-13-demo-image-urls.sql"));

        jdbc.execute(repair);
        jdbc.execute(repair);

        assertThat(jdbc.queryForObject("SELECT cover_url FROM restaurants WHERE id = 1", String.class))
                .isEqualTo("https://cdn.example.test/custom-cover.jpg");
        assertThat(jdbc.queryForObject("SELECT cover_url FROM restaurants WHERE id = 2", String.class))
                .isEqualTo("/menu-items-imgs/harvest-grain-bowl.jpg");
        assertThat(jdbc.queryForObject("SELECT photo_url FROM menu_items WHERE id = 1", String.class))
                .isEqualTo("/menu-items-imgs/maple-glazed-chicken-sandwich.jpg");
        assertThat(jdbc.queryForObject("SELECT photo_url FROM menu_items WHERE id = 2", String.class))
                .isEqualTo("https://cdn.example.test/custom-menu.jpg");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(8);
    }

    @Test
    void staleAssignmentRepairPreservesActiveDeliveriesAndAssignmentHistory() throws Exception {
        service(new ClassPathResource("demo/seed.sql")).reset();
        jdbc.update("INSERT INTO assignments (order_id, courier_id) VALUES (7, 7)");
        jdbc.update("UPDATE courier_profiles SET status = 'assigned' WHERE user_id IN (6, 7)");
        jdbc.update("UPDATE orders SET courier_id = 6 WHERE id = 5");
        jdbc.update("INSERT INTO assignments (order_id, courier_id, status, accepted_at) VALUES (5, 6, 'accepted', CURRENT_TIMESTAMP)");
        String repair = Files.readString(Path.of("docs/migrations/2026-11-13-close-stale-assignments.sql"));

        jdbc.execute(repair);
        jdbc.execute(repair);

        assertThat(jdbc.queryForObject("SELECT status FROM assignments WHERE order_id = 7", String.class)).isEqualTo("ended");
        assertThat(jdbc.queryForObject("SELECT status FROM courier_profiles WHERE user_id = 7", String.class)).isEqualTo("idle");
        assertThat(jdbc.queryForObject("SELECT status FROM assignments WHERE order_id = 5", String.class)).isEqualTo("accepted");
        assertThat(jdbc.queryForObject("SELECT status FROM courier_profiles WHERE user_id = 6", String.class)).isEqualTo("assigned");
        assertThat(jdbc.queryForObject("SELECT courier_id FROM orders WHERE id = 5", Long.class)).isEqualTo(6L);

        jdbc.update("INSERT INTO assignments (order_id, courier_id, status, accepted_at) VALUES (7, 7, 'accepted', CURRENT_TIMESTAMP)");
        jdbc.update("UPDATE courier_profiles SET status = 'carrying' WHERE user_id = 7");
        jdbc.execute(repair);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM assignments WHERE order_id = 7 AND status = 'ended'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM assignments WHERE order_id = 7 AND status = 'completed'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM courier_profiles WHERE user_id = 7", String.class)).isEqualTo("idle");
    }

    @Test
    void failureAfterRealTruncateRollsBackRowsAndKeepsSessionsValid() throws Exception {
        var realSeed = new ClassPathResource("demo/seed.sql");
        service(realSeed).reset();
        org.mockito.Mockito.clearInvocations(dashboards);
        jdbc.update("UPDATE menu_items SET price = 70 WHERE id = 3");
        String token = sessions.create(8L, Role.OPERATOR).token();
        String sql = realSeed.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
                .replace("COMMIT;", "INSERT INTO table_that_does_not_exist VALUES (1);\nCOMMIT;");

        assertThatThrownBy(() -> service(new ByteArrayResource(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8))).reset())
                .isInstanceOf(org.springframework.jdbc.datasource.init.ScriptStatementFailedException.class);

        assertThat(jdbc.queryForObject("SELECT price FROM menu_items WHERE id = 3", java.math.BigDecimal.class))
                .isEqualByComparingTo("70.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ledger_entries", Integer.class)).isEqualTo(8);
        assertThat(sessions.find(token)).isPresent();
        verifyNoInteractions(dashboards);
    }
}
