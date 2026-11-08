package com.council.tenderapp.dash;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MapControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private RestaurantRepository restaurants;

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
    void returnsRestaurantsOnDutyCouriersAndBoundsAroundThem() throws Exception {
        Account viewer = register("map-viewer@example.com", Role.STAFF);
        Account carrying = register("map-carrying@example.com", Role.COURIER);
        Account idle = register("map-idle@example.com", Role.COURIER);
        Account offline = register("map-offline@example.com", Role.COURIER);
        placeCourier(carrying, CourierStatus.CARRYING, 80.0, -170.0);
        placeCourier(idle, CourierStatus.IDLE, 45.51, -73.57);
        placeCourier(offline, CourierStatus.OFFLINE, 45.52, -73.56);
        // Farther out than any seeded location, so these two points set the bounds
        Restaurant far = restaurants.saveAndFlush(new Restaurant("Far Away Diner", "closed", -10.0, 170.0));

        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + viewer.token()))
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

    @Test
    void requiresALoggedInSession() throws Exception {
        mockMvc.perform(get("/api/map")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}
