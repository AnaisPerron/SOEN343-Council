package com.council.tenderapp.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.operator.reset.ResetCoordinator;
import com.council.tenderapp.operator.reset.ResetSeedLoader;
import com.council.tenderapp.operator.services.OperatorResetService;
import com.council.tenderapp.ord.observers.DashboardObserver;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

class OperatorResetServiceTest {
    private DriverManagerDataSource datasource;
    private JdbcTemplate jdbc;
    private SessionService sessions;
    private DashboardObserver dashboards;
    private ResetCoordinator coordinator;
    private String operatorToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        datasource = new DriverManagerDataSource("jdbc:h2:mem:reset-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(datasource);
        jdbc.execute("CREATE TABLE reset_fixture (id BIGINT PRIMARY KEY, name VARCHAR(100))");
        jdbc.update("INSERT INTO reset_fixture VALUES (1, 'before reset')");
        sessions = new SessionService(Duration.ofHours(24));
        operatorToken = sessions.create(8L, Role.OPERATOR).token();
        customerToken = sessions.create(1L, Role.CUSTOMER).token();
        dashboards = mock(DashboardObserver.class);
        coordinator = new ResetCoordinator();
    }

    private OperatorResetService service(String sql, boolean enabled) {
        var resource = new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8));
        return new OperatorResetService(coordinator, new ResetSeedLoader(jdbc, resource), sessions, dashboards,
                new DataSourceTransactionManager(datasource), enabled);
    }

    @Test
    void reloadsBaselineAndInvalidatesAllSessionsOnlyAfterCommit() {
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(jdbc.queryForObject("SELECT name FROM reset_fixture WHERE id = 1", String.class))
                    .isEqualTo("baseline");
            assertThat(sessions.find(operatorToken)).isEmpty();
            return null;
        }).when(dashboards).completeAll();
        service("BEGIN;\nDELETE FROM reset_fixture;\nINSERT INTO reset_fixture VALUES (1, 'baseline');\nCOMMIT;", true).reset();
        assertThat(jdbc.queryForObject("SELECT name FROM reset_fixture WHERE id = 1", String.class)).isEqualTo("baseline");
        assertThat(sessions.find(operatorToken)).isEmpty();
        assertThat(sessions.find(customerToken)).isEmpty();
        verify(dashboards).completeAll();
    }

    @Test
    void failedSeedRollsBackEarlierDeletesAndInsertsWithoutInvalidatingSessions() {
        assertThatThrownBy(() -> service("BEGIN;\nDELETE FROM reset_fixture;\n"
                + "INSERT INTO reset_fixture VALUES (2, 'partial');\n"
                + "INSERT INTO reset_fixture VALUES (2, 'duplicate');\nCOMMIT;", true).reset())
                .isInstanceOf(org.springframework.jdbc.datasource.init.ScriptStatementFailedException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reset_fixture", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT name FROM reset_fixture WHERE id = 1", String.class)).isEqualTo("before reset");
        assertThat(sessions.find(operatorToken)).isPresent();
        assertThat(sessions.find(customerToken)).isPresent();
        verifyNoInteractions(dashboards);
        try (var guard = coordinator.request()) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reset_fixture", Long.class)).isEqualTo(1);
        }
    }

    @Test
    void disabledResetDoesNotExecuteTheSeed() {
        assertThatThrownBy(() -> service("DELETE FROM reset_fixture;", false).reset())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(409));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reset_fixture", Long.class)).isEqualTo(1);
        assertThat(sessions.find(operatorToken)).isPresent();
        verifyNoInteractions(dashboards);
    }

    @Test
    void emptySeedDoesNotChangeData() {
        assertThatThrownBy(() -> service("BEGIN;\nCOMMIT;", true).reset()).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT name FROM reset_fixture WHERE id = 1", String.class)).isEqualTo("before reset");
        assertThat(sessions.find(customerToken)).isPresent();
    }
}
