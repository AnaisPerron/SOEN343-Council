package com.council.tenderapp.dash;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Profile;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;
import com.council.tenderapp.reg.services.SessionService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MapControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;
    @Autowired private SessionService sessions;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private OrderRepository orders;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ProfileRepository profiles;

    private record Account(Long id, String token) {}

    private Account register(String email, Role role) {
        Long id = registrationService.register(new RegisterRequest("Map Tester", email, "secret123", "514-555-0180", role)).id();
        return new Account(id, loginService.login(new LoginRequest(email, "secret123")).token());
    }

    private void placeCourier(Account courier, CourierStatus status, Double latitude, Double longitude) {
        CourierProfile profile = couriers.findById(courier.id()).orElseGet(() -> new CourierProfile(courier.id()));
        profile.setStatus(status);
        profile.setLatitude(latitude);
        profile.setLongitude(longitude);
        couriers.saveAndFlush(profile);
    }

    @Test
    void theOperatorSeesEveryRestaurantAndOnDutyCourier() throws Exception {
        // Operators are predefined, so the test opens an operator session directly
        String operatorToken = sessions.create(8L, Role.OPERATOR).token();
        Account carrying = register("map-carrying@example.com", Role.COURIER);
        Account idle = register("map-idle@example.com", Role.COURIER);
        Account offline = register("map-offline@example.com", Role.COURIER);
        placeCourier(carrying, CourierStatus.CARRYING, 80.0, -170.0);
        placeCourier(idle, CourierStatus.IDLE, 45.51, -73.57);
        placeCourier(offline, CourierStatus.OFFLINE, 45.52, -73.56);
        // Farther out than any seeded location, so these two points set the bounds
        Restaurant far = restaurants.saveAndFlush(new Restaurant("Far Away Diner", "closed", -10.0, 170.0));

        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bounds.minLatitude").value(-10.0))
                .andExpect(jsonPath("$.bounds.minLongitude").value(-170.0))
                .andExpect(jsonPath("$.bounds.maxLatitude").value(80.0))
                .andExpect(jsonPath("$.bounds.maxLongitude").value(170.0))
                .andExpect(jsonPath("$.restaurants[?(@.id == %d)].name", far.getId()).value(contains("Far Away Diner")))
                .andExpect(jsonPath("$.restaurants[?(@.id == %d)].status", far.getId()).value(contains("closed")))
                .andExpect(jsonPath("$.couriers[?(@.courierId == %d)].name", carrying.id()).value(contains("Map Tester")))
                .andExpect(jsonPath("$.couriers[?(@.courierId == %d)].status", carrying.id()).value(contains("carrying")))
                .andExpect(jsonPath("$.couriers[?(@.courierId == %d)].latitude", carrying.id()).value(contains(80.0)))
                .andExpect(jsonPath("$.couriers[?(@.courierId == %d)].status", idle.id()).value(contains("idle")))
                .andExpect(jsonPath("$.couriers[*].courierId").value(not(hasItem(offline.id().intValue()))));
    }

    @Test
    void leavesOutCouriersWithNoKnownLocation() throws Exception {
        Account viewer = register("map-viewer-2@example.com", Role.COURIER);
        Account courier = register("map-unplaced@example.com", Role.COURIER);
        placeCourier(courier, CourierStatus.IDLE, null, null);

        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + viewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couriers[?(@.courierId == %d)]", courier.id()).value(empty()));
    }

    // Customer registration upserts with ON CONFLICT, which H2 can't run, so the customer is created directly
    private Account customerAt(String email, double latitude, double longitude) {
        Long id = profiles.saveAndFlush(new Profile(Role.CUSTOMER, "Map Customer", email, "hash", "514-555-0181")).getId();
        jdbc.update("INSERT INTO customer_profiles (user_id, latitude, longitude) VALUES (?, ?, ?)", id, latitude, longitude);
        return new Account(id, sessions.create(id, Role.CUSTOMER).token());
    }

    @Test
    void customersAndCouriersOnlySeeTheirOwnDelivery() throws Exception {
        Account customer = customerAt("map-scope-customer@example.com", 45.53, -73.59);
        Account courier = register("map-scope-courier@example.com", Role.COURIER);
        Account otherCourier = register("map-scope-other@example.com", Role.COURIER);
        placeCourier(courier, CourierStatus.CARRYING, 45.52, -73.58);
        placeCourier(otherCourier, CourierStatus.IDLE, 45.50, -73.56);
        Restaurant pickup = restaurants.saveAndFlush(new Restaurant("Scope Cafe", "open", 45.51, -73.57));
        Restaurant elsewhere = restaurants.saveAndFlush(new Restaurant("Other Cafe", "open", 45.505, -73.565));
        Restaurant closed = restaurants.saveAndFlush(new Restaurant("Closed Cafe", "closed", 45.506, -73.566));
        Order order = new Order(customer.id(), pickup.getId(), OrderStatus.PICKED_UP, new BigDecimal("20.00"));
        order.setCourierId(courier.id());
        orders.saveAndFlush(order);

        // The customer sees the open restaurants, the courier bringing their order and themselves, but no other courier
        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurants[*].id").value(hasItem(elsewhere.getId().intValue())))
                .andExpect(jsonPath("$.restaurants[*].id").value(not(hasItem(closed.getId().intValue()))))
                .andExpect(jsonPath("$.couriers[*].courierId").value(contains(courier.id().intValue())))
                .andExpect(jsonPath("$.customers[*].customerId").value(contains(customer.id().intValue())));

        // The courier sees themselves, where to pick up and where to drop off
        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurants[*].id").value(contains(pickup.getId().intValue())))
                .andExpect(jsonPath("$.couriers[*].courierId").value(contains(courier.id().intValue())))
                .andExpect(jsonPath("$.customers[*].customerId").value(contains(customer.id().intValue())));

        // A courier with no order sees only themselves
        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCourier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurants").value(empty()))
                .andExpect(jsonPath("$.couriers[*].courierId").value(contains(otherCourier.id().intValue())))
                .andExpect(jsonPath("$.customers").value(empty()));
    }

    @Test
    void requiresALoggedInSession() throws Exception {
        mockMvc.perform(get("/api/map")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}
