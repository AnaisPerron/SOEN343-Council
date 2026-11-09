package com.council.tenderapp.reg.services;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// Orders reference customer_profiles, so every customer needs a row there before ordering.
// Uses JDBC because customer_profiles has no JPA entity.
@Service
public class CustomerProfileService {
    private final JdbcTemplate jdbc;

    public CustomerProfileService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Idempotent; also backfills customers who registered before profiles were created at sign-up.
    public void ensureExists(Long userId) {
        jdbc.update("INSERT INTO customer_profiles (user_id) VALUES (?) ON CONFLICT (user_id) DO NOTHING", userId);
    }

    /** Where the customer's orders are delivered; the location (when known) also places them on the city map. */
    public record DeliveryAddress(String address, Double latitude, Double longitude) {}

    public void saveAddress(Long userId, String address, Double latitude, Double longitude) {
        jdbc.update("UPDATE customer_profiles SET address = ?, latitude = ?, longitude = ? WHERE user_id = ?",
                address, latitude, longitude, userId);
    }

    public Optional<DeliveryAddress> deliveryAddress(Long userId) {
        return jdbc.query("SELECT address, latitude, longitude FROM customer_profiles WHERE user_id = ?",
                (rs, row) -> new DeliveryAddress(rs.getString("address"),
                        rs.getObject("latitude", Double.class), rs.getObject("longitude", Double.class)),
                userId).stream().findFirst();
    }
}
