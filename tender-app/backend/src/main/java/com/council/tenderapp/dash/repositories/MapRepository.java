package com.council.tenderapp.dash.repositories;

import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.council.tenderapp.dash.dtos.MapCourierDto;
import com.council.tenderapp.dash.dtos.MapRestaurantDto;

/** Read-only view of the locations the shared map shows; anything without a known location can't be placed. */
@Repository
public class MapRepository {
    private final JdbcTemplate jdbc;

    public MapRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MapRestaurantDto> restaurants() {
        return jdbc.query("""
                SELECT id, name, status, latitude, longitude
                FROM restaurants
                WHERE latitude IS NOT NULL AND longitude IS NOT NULL
                ORDER BY id
                """, (rs, row) -> new MapRestaurantDto(rs.getLong("id"), rs.getString("name"),
                        lower(rs.getString("status")), rs.getDouble("latitude"), rs.getDouble("longitude")));
    }

    // Offline couriers are off duty and not shown
    public List<MapCourierDto> onDutyCouriers() {
        return jdbc.query("""
                SELECT c.user_id, u.name, c.status, c.latitude, c.longitude
                FROM courier_profiles c JOIN users u ON u.id = c.user_id
                WHERE LOWER(c.status) <> 'offline' AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
                ORDER BY c.user_id
                """, (rs, row) -> new MapCourierDto(rs.getLong("user_id"), rs.getString("name"), lower(rs.getString("status")),
                        rs.getDouble("latitude"), rs.getDouble("longitude")));
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
