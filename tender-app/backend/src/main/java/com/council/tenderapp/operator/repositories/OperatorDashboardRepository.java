package com.council.tenderapp.operator.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.council.tenderapp.operator.dto.CourierSummary;
import com.council.tenderapp.operator.dto.CustomerSummary;
import com.council.tenderapp.operator.dto.DashboardPage;
import com.council.tenderapp.operator.dto.DashboardSummary;
import com.council.tenderapp.operator.dto.OrderSummary;
import com.council.tenderapp.operator.dto.RestaurantSummary;

@Repository
public class OperatorDashboardRepository {
    private final JdbcTemplate jdbc;

    public OperatorDashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardSummary summary() {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM restaurants) AS restaurant_count,
                    (SELECT COUNT(*) FROM orders) AS order_count,
                    (SELECT COUNT(*) FROM users WHERE role = 'customer') AS customer_count,
                    (SELECT COUNT(*) FROM users WHERE role = 'courier') AS courier_count,
                    (SELECT COUNT(*) FROM users u JOIN courier_profiles c ON c.user_id = u.id
                        WHERE u.role = 'courier' AND c.status = 'idle') AS available_courier_count
                """, (rs, row) -> new DashboardSummary(rs.getLong("restaurant_count"),
                        rs.getLong("order_count"), rs.getLong("customer_count"),
                        rs.getLong("courier_count"), rs.getLong("available_courier_count")));
    }

    public DashboardPage<RestaurantSummary> restaurants(int page, int size) {
        return page("SELECT COUNT(*) FROM restaurants", """
                SELECT id, name, status, latitude, longitude, address, phone
                FROM restaurants ORDER BY id LIMIT ? OFFSET ?
                """, (rs, row) -> new RestaurantSummary(rs.getLong("id"), rs.getString("name"),
                        lower(rs.getString("status")), number(rs, "latitude"), number(rs, "longitude"),
                        rs.getString("address"), rs.getString("phone")), page, size);
    }

    public DashboardPage<CustomerSummary> customers(int page, int size) {
        return page("SELECT COUNT(*) FROM users WHERE role = 'customer'", """
                SELECT u.id, u.name, u.email, u.phone,
                    (CASE WHEN c.user_id IS NULL THEN NULL ELSE COALESCE(c.td_cash_balance, 0) + COALESCE((SELECT SUM(t.tenders) FROM tender_cash_transactions t WHERE t.customer_id = u.id), 0) / 10.0 END) AS td_cash_balance,
                    c.latitude, c.longitude
                FROM users u LEFT JOIN customer_profiles c ON c.user_id = u.id
                WHERE u.role = 'customer' ORDER BY u.id LIMIT ? OFFSET ?
                """, (rs, row) -> new CustomerSummary(rs.getLong("id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("phone"), rs.getBigDecimal("td_cash_balance"),
                        number(rs, "latitude"), number(rs, "longitude")), page, size);
    }

    public DashboardPage<CourierSummary> couriers(int page, int size) {
        return page("SELECT COUNT(*) FROM users WHERE role = 'courier'", """
                SELECT u.id, u.name, u.email, u.phone, c.status, c.latitude, c.longitude
                FROM users u LEFT JOIN courier_profiles c ON c.user_id = u.id
                WHERE u.role = 'courier' ORDER BY u.id LIMIT ? OFFSET ?
                """, (rs, row) -> new CourierSummary(rs.getLong("id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("phone"), lower(rs.getString("status")),
                        number(rs, "latitude"), number(rs, "longitude")), page, size);
    }

    public DashboardPage<OrderSummary> orders(int page, int size) {
        return page("SELECT COUNT(*) FROM orders", """
                SELECT o.id, o.customer_id, o.restaurant_id, o.courier_id, o.status,
                    o.created_at, o.accepted_at, o.rejection_reason,
                    COALESCE(o.total_price, (SELECT SUM(l.quantity * l.unit_price) FROM order_lines l
                        WHERE l.order_id = o.id), 0) AS calculated_total
                FROM orders o ORDER BY o.id LIMIT ? OFFSET ?
                """, (rs, row) -> new OrderSummary(rs.getLong("id"), rs.getObject("customer_id", Long.class),
                        rs.getObject("restaurant_id", Long.class), rs.getObject("courier_id", Long.class),
                        lower(rs.getString("status")), instant(rs, "created_at"), instant(rs, "accepted_at"),
                        rs.getString("rejection_reason"), rs.getBigDecimal("calculated_total")), page, size);
    }

    private <T> DashboardPage<T> page(String countSql, String sql, RowMapper<T> mapper, int page, int size) {
        long total = jdbc.queryForObject(countSql, Long.class);
        return new DashboardPage<>(jdbc.query(sql, mapper, size, (long) page * size), total, page, size);
    }

    private static Double number(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, Double.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
