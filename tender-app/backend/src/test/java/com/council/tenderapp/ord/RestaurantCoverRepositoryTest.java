package com.council.tenderapp.ord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.RestaurantRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RestaurantCoverRepositoryTest {
    @Autowired private RestaurantRepository restaurants;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void usesCoverWithoutAnyMenuAndFallsBackToFirstNonemptyMenuPhoto() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS menus (id BIGINT PRIMARY KEY, restaurant_id BIGINT NOT NULL)");
        long id = restaurants.saveAndFlush(new Restaurant("Cover Cafe", "open", 45.5, -73.5)).getId();
        jdbc.update("UPDATE restaurants SET cover_url = '/menu-items-imgs/cover.jpg' WHERE id = ?", id);
        assertThat(photo(id)).isEqualTo("/menu-items-imgs/cover.jpg");

        jdbc.update("INSERT INTO menus (id, restaurant_id) VALUES (99001, ?)", id);
        jdbc.update("INSERT INTO menu_items (id, menu_id, name, price, status, photo_url) VALUES "
                + "(99001,99001,'Empty',10,'available',''),"
                + "(99002,99001,'First',10,'available','/menu-items-imgs/first.jpg'),"
                + "(99003,99001,'Second',10,'available','/menu-items-imgs/second.jpg')");
        assertThat(photo(id)).isEqualTo("/menu-items-imgs/cover.jpg");
        jdbc.update("UPDATE restaurants SET cover_url = NULL WHERE id = ?", id);
        assertThat(photo(id)).isEqualTo("/menu-items-imgs/first.jpg");
    }

    private String photo(long id) {
        return restaurants.findRestaurantBrowseByStatus("open").stream()
                .filter(restaurant -> restaurant.getId() == id).findFirst().orElseThrow().getPhotoUrl();
    }
}
