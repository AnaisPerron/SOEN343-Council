package com.council.tenderapp.dash;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.council.tenderapp.dash.repositories.MapRepository;

class MapRepositoryTest {
    @Test
    void restaurantMarkersCountCurrentOrdersOnly() throws Exception {
        var datasource = new DriverManagerDataSource(
                "jdbc:h2:mem:map-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        try (var connection = datasource.getConnection()) {
            new ResourceDatabasePopulator(new ClassPathResource("operator-dashboard.sql")).execute(datasource);
            var jdbc = new JdbcTemplate(datasource);
            jdbc.update("INSERT INTO orders (id, restaurant_id, status) VALUES "
                    + "(40, 10, 'accepted'), (50, 10, 'preparing'), (60, 10, 'ready'), (70, 10, 'cancelled')");

            var restaurants = new MapRepository(jdbc).restaurants();

            assertThat(restaurants).hasSize(1);
            assertThat(restaurants.get(0).queueCount()).isEqualTo(3);
        }
    }
}
