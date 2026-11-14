package com.council.tenderapp.dash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;
import com.jayway.jsonpath.JsonPath;

// Not @Transactional: the courier is released after the order change commits, so each step must really commit.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:map-sync;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class MapSyncIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;
    @Autowired private CourierDutyService courierDutyService;
    @Autowired private OrderService orderService;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private OrderRepository orders;

    /** The courier's state as the map shows it, or null when the courier is not on the map. */
    private String mapStatusOf(Long courierId, String token) throws Exception {
        String body = mockMvc.perform(get("/api/map").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> found = JsonPath.read(body, "$.couriers[?(@.courierId == " + courierId + ")].status");
        return found.isEmpty() ? null : found.get(0);
    }

    @Test
    void mapFollowsTheCourierThroughDutyAssignmentPickupAndDelivery() throws Exception {
        Long courierId = registrationService.register(new RegisterRequest(
                "Sync Tester", "map-sync-courier@example.com", "secret123", "514-555-0190", Role.COURIER)).id();
        String token = loginService.login(new LoginRequest("map-sync-courier@example.com", "secret123")).token();
        CourierProfile profile = couriers.findById(courierId).orElseGet(() -> new CourierProfile(courierId));
        profile.setLatitude(45.51);
        profile.setLongitude(-73.57);
        couriers.saveAndFlush(profile);
        Long restaurantId = restaurants.saveAndFlush(new Restaurant("Sync Cafe", "open", 45.50, -73.57)).getId();
        Long orderId = null;
        try {
            // Off duty: not on the map
            assertThat(mapStatusOf(courierId, token)).isNull();

            // Going on duty receives the ready order as an offer
            orderId = orders.saveAndFlush(new Order(restaurantId, new Date(), OrderStatus.READY)).getId();
            courierDutyService.goOnDuty(courierId);
            assertThat(mapStatusOf(courierId, token)).isEqualTo("assigned");
            courierDutyService.acceptOffer(courierId, orderId);
            assertThat(mapStatusOf(courierId, token)).isEqualTo("assigned");

            // Pickup puts the order in the courier's hands
            orderService.markPickedUpByCourier(courierId, orderId);
            assertThat(mapStatusOf(courierId, token)).isEqualTo("carrying");

            // Delivering frees the courier
            orderService.markDeliveredByCourier(courierId, orderId);
            assertThat(mapStatusOf(courierId, token)).isEqualTo("idle");

            courierDutyService.goOffDuty(courierId);
            assertThat(mapStatusOf(courierId, token)).isNull();
        } finally {
            // Leave no idle courier or waiting order behind for other tests' dispatch runs
            if (orderId != null) {
                Long id = orderId;
                orders.findById(id).filter(o -> o.getStatus() == OrderStatus.READY)
                        .ifPresent(o -> orderService.refundOrder(restaurantId, id));
            }
            if (couriers.findById(courierId).orElseThrow().getStatus() == CourierStatus.IDLE) courierDutyService.goOffDuty(courierId);
        }
    }
}
