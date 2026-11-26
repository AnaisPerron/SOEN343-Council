package com.council.tenderapp.rvw;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.council.tenderapp.ord.dtos.RestaurantResponseDTO;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.BrowseService;
import com.council.tenderapp.rvw.dtos.RestaurantRatingSummary;
import com.council.tenderapp.rvw.entities.OrderRating;
import com.council.tenderapp.rvw.repositories.OrderRatingRepository;
import com.council.tenderapp.rvw.services.RestaurantRatingService;

@SpringBootTest
class RestaurantRatingAverageIntegrationTest {
    @Autowired private RestaurantRatingService service;
    @Autowired private BrowseService browse;
    @Autowired private OrderRatingRepository ratings;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> restaurantIds = new ArrayList<>();
    private long nextOrderId = 880_000;

    @BeforeEach
    void browseQueryNeedsMenus() {
        // The browse query reads menus, which has no JPA entity in the test schema.
        jdbc.execute("CREATE TABLE IF NOT EXISTS menus (id BIGINT PRIMARY KEY, restaurant_id BIGINT NOT NULL)");
    }

    @AfterEach
    void tearDown() {
        for (Long id : restaurantIds) {
            jdbc.update("DELETE FROM order_ratings WHERE restaurant_id = ?", id);
            restaurants.deleteById(id);
        }
    }

    private Long restaurant(String name) {
        Long id = restaurants.saveAndFlush(new Restaurant(name, "open", 45.5, -73.5)).getId();
        restaurantIds.add(id);
        return id;
    }

    private void rate(Long restaurantId, int stars) {
        ratings.saveAndFlush(new OrderRating(nextOrderId++, 1L, restaurantId, null, stars, null, null, Instant.now()));
    }

    @Test
    void averageIsTheMeanOfRestaurantStarsRoundedHalfUpToOneDecimal() {
        Long id = restaurant("Average Diner");
        for (int stars : new int[] {5, 4, 4, 4}) rate(id, stars); // 17 / 4 = 4.25

        assertThat(service.summaries(List.of(id)))
                .containsEntry(id, new RestaurantRatingSummary(4.3, 4));
    }

    @Test
    void restaurantWithNoRatingsHasNullAverageAndZeroCount() {
        Long id = restaurant("Brand New Bistro");
        assertThat(service.summaries(List.of(id))).containsEntry(id, RestaurantRatingSummary.NONE);
        assertThat(RestaurantRatingSummary.NONE.averageRating()).isNull();
        assertThat(RestaurantRatingSummary.NONE.ratingCount()).isZero();
        assertThat(service.summaries(List.of())).isEmpty();
    }

    @Test
    void starsOutsideOneToFiveAreIgnoredAndRestaurantsAreKeptApart() {
        Long rated = restaurant("Rated Place");
        Long other = restaurant("Other Place");
        rate(rated, 2);
        rate(rated, 3);
        rate(other, 5);
        // Bypasses the entity check to simulate a bad row written outside the application.
        jdbc.update("INSERT INTO order_ratings (order_id, customer_id, restaurant_id, restaurant_stars, created_at) "
                + "VALUES (?, 1, ?, 0, CURRENT_TIMESTAMP)", nextOrderId++, rated);

        Map<Long, RestaurantRatingSummary> summaries = service.summaries(List.of(rated, other));
        assertThat(summaries.get(rated)).isEqualTo(new RestaurantRatingSummary(2.5, 2));
        assertThat(summaries.get(other)).isEqualTo(new RestaurantRatingSummary(5.0, 1));
    }

    @Test
    void restaurantResponseCarriesTheAverageAndHandlesUnratedRestaurants() {
        Long rated = restaurant("Browse Rated");
        Long unrated = restaurant("Browse Unrated");
        rate(rated, 5);
        rate(rated, 4);

        List<RestaurantResponseDTO> open = browse.getOpenRestaurants();
        RestaurantResponseDTO ratedDto = open.stream().filter(r -> r.id().equals(rated)).findFirst().orElseThrow();
        RestaurantResponseDTO unratedDto = open.stream().filter(r -> r.id().equals(unrated)).findFirst().orElseThrow();

        assertThat(ratedDto.averageRating()).isEqualTo(4.5);
        assertThat(ratedDto.ratingCount()).isEqualTo(2);
        assertThat(unratedDto.averageRating()).isNull();
        assertThat(unratedDto.ratingCount()).isZero();
    }
}
