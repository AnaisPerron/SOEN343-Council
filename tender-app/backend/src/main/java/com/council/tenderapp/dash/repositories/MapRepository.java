package com.council.tenderapp.dash.repositories;

import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.council.tenderapp.dash.dtos.MapCourierDto;
import com.council.tenderapp.dash.dtos.MapCustomerDto;
import com.council.tenderapp.dash.dtos.MapRestaurantDto;

/** Read-only view of the locations the map shows; anything without a known location can't be placed. */
@Repository
public class MapRepository {
    // An order a courier is still working on; the map links its restaurant, courier and customer while it lasts
    private static final String ACTIVE_ORDER = "o.courier_id IS NOT NULL AND LOWER(o.status) IN ('accepted', 'preparing', 'ready', 'picked_up')";

    private final JdbcTemplate jdbc;

    public MapRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MapRestaurantDto> restaurants() {
        return restaurants("TRUE");
    }

    /** The restaurants a customer can order from. */
    public List<MapRestaurantDto> openRestaurants() {
        return restaurants("LOWER(r.status) = 'open'");
    }

    public List<MapRestaurantDto> restaurant(long restaurantId) {
        return restaurants("r.id = ?", restaurantId);
    }

    /** Where the courier picks up their current order. */
    public List<MapRestaurantDto> restaurantsOfCourierOrders(long courierId) {
        return restaurants("r.id IN (SELECT o.restaurant_id FROM orders o WHERE " + ACTIVE_ORDER + " AND o.courier_id = ?)", courierId);
    }

    // Offline couriers are off duty and not shown
    public List<MapCourierDto> onDutyCouriers() {
        return couriers("TRUE");
    }

    public List<MapCourierDto> onDutyCourier(long courierId) {
        return couriers("c.user_id = ?", courierId);
    }

    /** Couriers bringing the customer their orders. */
    public List<MapCourierDto> couriersOfCustomerOrders(long customerId) {
        return couriers("c.user_id IN (SELECT o.courier_id FROM orders o WHERE " + ACTIVE_ORDER + " AND o.customer_id = ?)", customerId);
    }

    /** Couriers coming for, or carrying, the restaurant's orders. */
    public List<MapCourierDto> couriersOfRestaurantOrders(long restaurantId) {
        return couriers("c.user_id IN (SELECT o.courier_id FROM orders o WHERE " + ACTIVE_ORDER + " AND o.restaurant_id = ?)", restaurantId);
    }

    public List<MapCustomerDto> customer(long customerId) {
        return customers("p.user_id = ?", customerId);
    }

    public List<MapCustomerDto> customers() {
        return customers("TRUE");
    }

    /** Where the courier delivers their current order. */
    public List<MapCustomerDto> customersOfCourierOrders(long courierId) {
        return customers("p.user_id IN (SELECT o.customer_id FROM orders o WHERE " + ACTIVE_ORDER + " AND o.courier_id = ?)", courierId);
    }

    private List<MapRestaurantDto> restaurants(String filter, Object... args) {
        return jdbc.query("""
                SELECT r.id, r.name, r.status, r.latitude, r.longitude,
                    (SELECT COUNT(*) FROM orders o WHERE o.restaurant_id = r.id
                        AND LOWER(o.status) IN ('placed', 'accepted', 'preparing')) AS queue_count
                FROM restaurants r
                WHERE r.latitude IS NOT NULL AND r.longitude IS NOT NULL AND (%s)
                ORDER BY r.id
                """.formatted(filter), (rs, row) -> new MapRestaurantDto(rs.getLong("id"), rs.getString("name"),
                        lower(rs.getString("status")), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getLong("queue_count")), args);
    }

    private List<MapCourierDto> couriers(String filter, Object... args) {
        return jdbc.query("""
                SELECT c.user_id, u.name, c.status, c.latitude, c.longitude
                FROM courier_profiles c JOIN users u ON u.id = c.user_id
                WHERE LOWER(c.status) <> 'offline' AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND (%s)
                ORDER BY c.user_id
                """.formatted(filter), (rs, row) -> new MapCourierDto(rs.getLong("user_id"), rs.getString("name"),
                        lower(rs.getString("status")), rs.getDouble("latitude"), rs.getDouble("longitude")), args);
    }

    private List<MapCustomerDto> customers(String filter, Object... args) {
        return jdbc.query("""
                SELECT p.user_id, u.name, p.latitude, p.longitude
                FROM customer_profiles p JOIN users u ON u.id = p.user_id
                WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND (%s)
                ORDER BY p.user_id
                """.formatted(filter), (rs, row) -> new MapCustomerDto(rs.getLong("user_id"), rs.getString("name"),
                        rs.getDouble("latitude"), rs.getDouble("longitude")), args);
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
