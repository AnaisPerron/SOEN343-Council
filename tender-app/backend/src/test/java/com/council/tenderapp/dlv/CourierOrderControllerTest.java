package com.council.tenderapp.dlv;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.council.tenderapp.reg.services.CustomerProfileService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
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
class CourierOrderControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private OrderRepository orders;
    @Autowired private OrderEventRepository orderEvents;
    @MockitoBean private CustomerProfileService customerProfileService;

    private record Account(Long id, String token) {}

    private Account register(String email, Role role) {
        Long id = registrationService.register(
                new RegisterRequest("Pickup Tester", email, "secret123", "514-555-0190", role)).id();
        return new Account(id, loginService.login(new LoginRequest(email, "secret123")).token());
    }

    private Order orderFor(Account courier, OrderStatus status) {
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Pickup Cafe", "open", 45.50, -73.57));
        Order order = new Order(restaurant.getId(), new Date(), status);
        order.setCourierId(courier.id());
        return orders.saveAndFlush(order);
    }

    private void setStatus(Account courier, CourierStatus status) {
        CourierProfile profile = couriers.findById(courier.id()).orElseThrow();
        profile.setStatus(status);
        couriers.saveAndFlush(profile);
    }

    private String pickupUrl(Long orderId) {
        return "/api/courier/orders/" + orderId + "/pickup";
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void assignedCourierPicksUpAReadyOrder() throws Exception {
        Account courier = register("pickup-happy@example.com", Role.COURIER);
        Order order = orderFor(courier, OrderStatus.READY);
        setStatus(courier, CourierStatus.ASSIGNED);
        mockMvc.perform(post("/api/courier/duty/orders/" + order.getId() + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk());


        mockMvc.perform(post(pickupUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.orderStatus").value("picked_up"))
                .andExpect(jsonPath("$.courierStatus").value("carrying"));

        Order saved = orders.findById(order.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        assertThat(saved.getPickupDate()).isNotNull();
        assertThat(couriers.findById(courier.id()).orElseThrow().getStatus()).isEqualTo(CourierStatus.CARRYING);
        assertThat(orderEvents.findAll()).anySatisfy(event -> {
            assertThat(event.getOrderId()).isEqualTo(order.getId());
            assertThat(event.getEventType()).isEqualTo("order_picked_up");
            assertThat(event.getPreviousStatus()).isEqualTo("ready");
            assertThat(event.getNewStatus()).isEqualTo("picked_up");
        });
    }

    @Test
    void refusesPickupUntilTheCourierAcceptsTheOffer() throws Exception {
        Account courier = register("pickup-unaccepted@example.com", Role.COURIER);
        Order order = orderFor(courier, OrderStatus.READY);
        setStatus(courier, CourierStatus.ASSIGNED);
        mockMvc.perform(post(pickupUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isConflict());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(couriers.findById(courier.id()).orElseThrow().getStatus()).isEqualTo(CourierStatus.ASSIGNED);
    }

    @Test
    void refusesAnOrderThatIsNotReady() throws Exception {
        Account courier = register("pickup-notready@example.com", Role.COURIER);
        Order order = orderFor(courier, OrderStatus.PREPARING);

        mockMvc.perform(post(pickupUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isConflict());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void refusesACourierWhoIsNotAssignedToTheOrder() throws Exception {
        Account owner = register("pickup-owner@example.com", Role.COURIER);
        Account other = register("pickup-other@example.com", Role.COURIER);
        Order order = orderFor(owner, OrderStatus.READY);

        mockMvc.perform(post(pickupUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isForbidden());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void refusesANonCourierRole() throws Exception {
        Account courier = register("pickup-assigned@example.com", Role.COURIER);
        Account customer = register("pickup-customer@example.com", Role.CUSTOMER);
        Order order = orderFor(courier, OrderStatus.READY);

        mockMvc.perform(post(pickupUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + customer.token()))
                .andExpect(status().isForbidden());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void refusesAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(post(pickupUrl(1L))).andExpect(status().isUnauthorized());
    }

    @Test
    void refusesAnUnknownOrder() throws Exception {
        Account courier = register("pickup-unknown@example.com", Role.COURIER);

        mockMvc.perform(post(pickupUrl(999_999L)).header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isNotFound());
    }
}