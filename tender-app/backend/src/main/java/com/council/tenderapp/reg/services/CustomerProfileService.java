package com.council.tenderapp.reg.services;

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
}
