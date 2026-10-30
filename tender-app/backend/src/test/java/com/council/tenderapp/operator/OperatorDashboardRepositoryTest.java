package com.council.tenderapp.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.council.tenderapp.operator.dto.CustomerSummary;
import com.council.tenderapp.operator.dto.DashboardSummary;
import com.council.tenderapp.operator.dto.OrderSummary;
import com.council.tenderapp.operator.dto.RestaurantSummary;
import com.council.tenderapp.operator.repositories.OperatorDashboardRepository;

class OperatorDashboardRepositoryTest {
    private Connection connection;
    private JdbcTemplate jdbc;
    private OperatorDashboardRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        var datasource = new DriverManagerDataSource(
                "jdbc:h2:mem:operator-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        connection = datasource.getConnection();
        new ResourceDatabasePopulator(new ClassPathResource("operator-dashboard.sql")).execute(datasource);
        jdbc = new JdbcTemplate(datasource);
        repository = new OperatorDashboardRepository(jdbc);
    }

    @AfterEach
    void tearDown() throws SQLException { connection.close(); }

    @Test
    void summaryCountsAllAccountsAndOnlyIdleCouriersAsAvailable() {
        assertThat(repository.summary()).isEqualTo(new DashboardSummary(2, 3, 2, 4, 1));
        repository.customers(0, 1);
        assertThat(repository.summary().customerCount()).isEqualTo(2);
    }

    @Test
    void restaurantsAreStablyPagedWithFullCountsAndNullableContactData() {
        var first = repository.restaurants(0, 1);
        var second = repository.restaurants(1, 1);
        assertThat(first.total()).isEqualTo(2);
        assertThat(first.page()).isZero();
        assertThat(first.size()).isEqualTo(1);
        assertThat(first.items()).extracting(RestaurantSummary::id).containsExactly(10L);
        assertThat(second.items()).extracting(RestaurantSummary::id).containsExactly(20L);
        assertThat(first.items().get(0).address()).isEqualTo("Main Street");
        assertThat(second.items().get(0).latitude()).isNull();
        assertThat(second.items().get(0).phone()).isNull();
        assertThat(repository.restaurants(2, 1).items()).isEmpty();
        assertThat(repository.restaurants(Integer.MAX_VALUE, 100).items()).isEmpty();
    }

    @Test
    void customersIncludeAccountsWithoutProfilesAndDoNotInventBalances() {
        var result = repository.customers(0, 50);
        assertThat(result.items()).extracting(CustomerSummary::id).containsExactly(1L, 2L);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items().get(0).tdCashBalance()).isEqualByComparingTo("25.50");
        assertThat(result.items().get(1).tdCashBalance()).isNull();
        assertThat(result.items().get(1).latitude()).isNull();
        assertThat(result.items().get(1).phone()).isNull();
    }

    @Test
    void couriersPreserveBusyOfflineAndUnknownAvailability() {
        var result = repository.couriers(0, 50);
        assertThat(result.total()).isEqualTo(4);
        assertThat(result.items()).extracting(c -> c.status()).containsExactly("idle", "carrying", "offline", null);
        assertThat(repository.couriers(1, 2).items()).extracting(c -> c.id()).containsExactly(5L, 8L);
    }

    @Test
    void orderTotalsFollowStoredTotalThenLineSnapshotsThenZero() {
        var result = repository.orders(0, 50);
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.items()).extracting(OrderSummary::id).containsExactly(10L, 20L, 30L);
        assertThat(result.items().get(0).totalPrice()).isEqualByComparingTo("99.00");
        assertThat(result.items().get(1).totalPrice()).isEqualByComparingTo("12.50");
        assertThat(result.items().get(2).totalPrice()).isEqualByComparingTo("0");
        jdbc.update("UPDATE orders SET total_price = NULL WHERE id = 10");
        assertThat(repository.orders(0, 50).items().get(0).totalPrice()).isEqualByComparingTo("25.00");
        assertThat(result.items().get(1).status()).isEqualTo("refunded");
        assertThat(repository.orders(1, 1).items()).extracting(OrderSummary::id).containsExactly(20L);
    }

    @Test
    void orderDatesAndMissingAssignmentsArePreserved() {
        var orders = repository.orders(0, 50).items();
        assertThat(orders.get(0).dateOrdered()).isEqualTo(Instant.parse("2026-10-29T12:00:00Z"));
        assertThat(orders.get(0).courierId()).isEqualTo(3);
        assertThat(orders.get(1).acceptedAt()).isEqualTo(Instant.parse("2026-10-29T13:01:00Z"));
        assertThat(orders.get(1).courierId()).isNull();
        assertThat(orders.get(2).customerId()).isNull();
        assertThat(orders.get(2).restaurantId()).isNull();
        assertThat(orders.get(2).dateOrdered()).isNull();
    }

    @Test
    void emptyDatabaseReturnsZeroCountsAndEmptyPages() {
        jdbc.update("DELETE FROM order_lines");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM courier_profiles");
        jdbc.update("DELETE FROM customer_profiles");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM restaurants");
        assertThat(repository.summary()).isEqualTo(new DashboardSummary(0, 0, 0, 0, 0));
        assertThat(repository.restaurants(0, 50).items()).isEmpty();
        assertThat(repository.orders(0, 50).items()).isEmpty();
        assertThat(repository.customers(0, 50).items()).isEmpty();
        assertThat(repository.couriers(0, 50).items()).isEmpty();
    }
}
