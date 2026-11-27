package com.council.tenderapp.rvw;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.services.SessionService;

/** RVW requirements through HTTP: real security, controllers, services and database (H2). */
@SpringBootTest
@AutoConfigureMockMvc
class RatingFlowEndToEndTest {
    private static final long ALICE = 1;
    private static final long OMAR = 2;
    private static final long COURIER = 6;

    @Autowired private MockMvc mockMvc;
    @Autowired private SessionService sessions;
    @Autowired private OrderRepository orders;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> orderIds = new ArrayList<>();
    private Long restaurantId;

    @BeforeEach
    void setUp() {
        // The browse query reads menus, which has no JPA entity in the test schema.
        jdbc.execute("CREATE TABLE IF NOT EXISTS menus (id BIGINT PRIMARY KEY, restaurant_id BIGINT NOT NULL)");
        restaurantId = restaurants.saveAndFlush(new Restaurant("Rated Ramen", "open", 45.5, -73.5)).getId();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM order_ratings WHERE restaurant_id = ?", restaurantId);
        orderIds.forEach(orders::deleteById);
        restaurants.deleteById(restaurantId);
    }

    private Long order(long customerId, OrderStatus status) {
        Long id = orders.saveAndFlush(new Order(customerId, restaurantId, OrderStatus.PLACED, new BigDecimal("18.00"))).getId();
        jdbc.update("UPDATE orders SET status = ?, courier_id = ? WHERE id = ?",
                status.name().toLowerCase(Locale.ROOT), COURIER, id);
        orderIds.add(id);
        return id;
    }

    private String bearer(long id, Role role) {
        return "Bearer " + sessions.create(id, role).token();
    }

    private ResultActions rate(String bearer, Long orderId, int restaurantStars, int courierStars) throws Exception {
        return mockMvc.perform(post("/api/orders/{orderId}/rating", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"restaurantStars\":" + restaurantStars + ",\"courierStars\":" + courierStars + "}"));
    }

    private ResultActions openRestaurants(String bearer) throws Exception {
        return mockMvc.perform(get("/api/restaurant/open").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
    }

    @Test
    void acceptedRatingsProduceTheAverageAndASecondRatingIsRefused() throws Exception {
        String alice = bearer(ALICE, Role.CUSTOMER);
        String omar = bearer(OMAR, Role.CUSTOMER);
        Long first = order(ALICE, OrderStatus.DELIVERED);
        Long second = order(OMAR, OrderStatus.DELIVERED);

        openRestaurants(alice)
                .andExpect(jsonPath("$[?(@.id == " + restaurantId + ")].averageRating").value(contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id == " + restaurantId + ")].ratingCount").value(contains(0)));

        rate(alice, first, 5, 4).andExpect(status().isCreated()).andExpect(jsonPath("$.restaurantStars").value(5));
        rate(omar, second, 4, 4).andExpect(status().isCreated());
        rate(alice, first, 1, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order " + first + " has already been rated"));

        // (5 + 4) / 2; the refused third attempt does not count.
        openRestaurants(alice)
                .andExpect(jsonPath("$[?(@.id == " + restaurantId + ")].averageRating").value(contains(4.5)))
                .andExpect(jsonPath("$[?(@.id == " + restaurantId + ")].ratingCount").value(contains(2)));
    }

    @Test
    void anotherCustomersOrderAndUndeliveredOrdersAreRefusedAndLeaveTheAverageUnchanged() throws Exception {
        String alice = bearer(ALICE, Role.CUSTOMER);
        Long omarsOrder = order(OMAR, OrderStatus.DELIVERED);
        rate(alice, omarsOrder, 1, 1).andExpect(status().isForbidden());

        for (OrderStatus status : List.of(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING,
                OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.REFUNDED)) {
            rate(alice, order(ALICE, status), 1, 1).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.endsWith(status.name().toLowerCase(Locale.ROOT))));
        }

        openRestaurants(alice)
                .andExpect(jsonPath("$[?(@.id == " + restaurantId + ")].averageRating").value(contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id == " + restaurantId + ")].ratingCount").value(contains(0)));
    }

    @Test
    void restaurantCourierAndOperatorAccountsCannotRate() throws Exception {
        Long delivered = order(ALICE, OrderStatus.DELIVERED);
        for (Role role : List.of(Role.STAFF, Role.COURIER, Role.OPERATOR)) {
            rate(bearer(ALICE, role), delivered, 5, 5).andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/orders/{orderId}/rating", delivered).contentType(MediaType.APPLICATION_JSON)
                .content("{\"restaurantStars\":5,\"courierStars\":5}")).andExpect(status().isUnauthorized());
    }
}
