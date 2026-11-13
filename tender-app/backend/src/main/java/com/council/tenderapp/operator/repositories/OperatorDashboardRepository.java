package com.council.tenderapp.operator.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
    public record RestaurantOption(long id, String name) {}
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
        return restaurants(page, size, List.of());
    }

    public DashboardPage<RestaurantSummary> restaurants(int page, int size, List<Long> restaurantIds) {
        String where = restaurantIds == null || restaurantIds.isEmpty() ? "" :
                " WHERE r.id IN (" + placeholders(restaurantIds.size()) + ")";
        Object[] ids = restaurantIds == null ? new Object[0] : restaurantIds.toArray();
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM restaurants r" + where, Long.class, ids);
        List<Object> arguments = new ArrayList<>(restaurantIds == null ? List.of() : restaurantIds);
        arguments.add(size);
        arguments.add((long) page * size);
        List<RestaurantSummary> items = jdbc.query("""
                SELECT                 r.id, r.name, r.status, r.latitude, r.longitude, r.address, r.phone,
                (SELECT COUNT(*) FROM orders o WHERE o.restaurant_id = r.id
                    AND UPPER(o.status) IN ('PLACED', 'ACCEPTED', 'PREPARING')) AS queue_count
                FROM restaurants r
                """ + where + " ORDER BY LOWER(r.name), r.id LIMIT ? OFFSET ?", (rs, row) -> new RestaurantSummary(rs.getLong("id"), rs.getString("name"),
                        lower(rs.getString("status")), number(rs, "latitude"), number(rs, "longitude"),
                        rs.getString("address"), rs.getString("phone"), rs.getLong("queue_count")), arguments.toArray());
        return new DashboardPage<>(items, total, page, size);
    }

    public List<RestaurantOption> restaurantOptions() {
        return jdbc.query("SELECT id, name FROM restaurants ORDER BY LOWER(name), id",
                (rs, row) -> new RestaurantOption(rs.getLong("id"), rs.getString("name")));
    }

    public DashboardPage<CustomerSummary> customers(int page, int size) {
        return page("SELECT COUNT(*) FROM users WHERE role = 'customer'", """
                SELECT u.id, u.name, u.email, u.phone,
                    (CASE WHEN c.user_id IS NULL THEN NULL ELSE FLOOR(COALESCE(c.td_cash_balance, 0) * 10) + COALESCE((SELECT SUM(t.tenders) FROM tender_cash_transactions t WHERE t.customer_id = u.id), 0) END) AS td_cash_balance,
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

    public DashboardPage<OrderSummary> orders(int page, int size, List<String> statuses,
            List<Long> restaurantIds, LocalDate from, LocalDate to, ZoneId timeZone) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> arguments = new ArrayList<>();
        if (statuses != null && !statuses.isEmpty()) {
            where.append(" AND LOWER(o.status) IN (").append(placeholders(statuses.size())).append(")");
            arguments.addAll(statuses);
        }
        if (restaurantIds != null && !restaurantIds.isEmpty()) {
            where.append(" AND o.restaurant_id IN (").append(placeholders(restaurantIds.size())).append(")");
            arguments.addAll(restaurantIds);
        }
        if (from != null) {
            where.append(" AND o.created_at >= ?");
            arguments.add(java.sql.Timestamp.from(from.atStartOfDay(timeZone).toInstant()));
        }
        if (to != null) {
            where.append(" AND o.created_at < ?");
            arguments.add(java.sql.Timestamp.from(to.plusDays(1).atStartOfDay(timeZone).toInstant()));
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM orders o" + where, Long.class, arguments.toArray());
        List<Object> paged = new ArrayList<>(arguments);
        paged.add(size);
        paged.add((long) page * size);
        List<OrderSummary> items = jdbc.query("""
                SELECT o.id, o.customer_id, o.restaurant_id, o.courier_id, o.status,
                    o.created_at, o.accepted_at, o.rejection_reason,
                    COALESCE(o.total_price, (SELECT SUM(l.quantity * l.unit_price) FROM order_lines l
                        WHERE l.order_id = o.id), 0) AS calculated_total
                FROM orders o
                """ + where + " ORDER BY o.id DESC LIMIT ? OFFSET ?", (rs, row) -> new OrderSummary(rs.getLong("id"), rs.getObject("customer_id", Long.class),
                        rs.getObject("restaurant_id", Long.class), rs.getObject("courier_id", Long.class),
                        lower(rs.getString("status")), instant(rs, "created_at"), instant(rs, "accepted_at"),
                        rs.getString("rejection_reason"), rs.getBigDecimal("calculated_total")), paged.toArray());
        return new DashboardPage<>(items, total, page, size);
    }

    public DashboardPage<OrderSummary> orders(int page, int size) {
        return orders(page, size, List.of(), List.of(), null, null, ZoneOffset.UTC);
    }

    private static String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
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
