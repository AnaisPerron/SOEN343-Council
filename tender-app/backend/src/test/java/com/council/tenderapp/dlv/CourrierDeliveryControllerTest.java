package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.reg.dtos.LoginRequest;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.services.CustomerProfileService;
import com.council.tenderapp.reg.services.LoginService;
import com.council.tenderapp.reg.services.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CourierDeliveryControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;
    @Autowired private CourierDutyService courierDutyService;
    @Autowired private OrderService orderService;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private OrderRepository orders;
    @Autowired private OrderEventRepository orderEvents;
    @MockitoBean private CustomerProfileService customerProfileService;

    private record Account(Long id, String token) {}

    private Account register(String email, Role role) {
        Long id = registrationService.register(
                new RegisterRequest("Delivery Tester", email, "secret123", "514-555-0191", role)).id();
        return new Account(id, loginService.login(new LoginRequest(email, "secret123")).token());
    }

    private Restaurant restaurant() {
        return restaurants.saveAndFlush(new Restaurant("Delivery Cafe", "open", 45.50, -73.57));
    }

    private Order orderFor(Account courier, Restaurant restaurant, OrderStatus status) {
        Order order = new Order(restaurant.getId(), new Date(), status);
        order.setCourierId(courier.id());
        return orders.saveAndFlush(order);
    }

    private CourierStatus statusOf(Account courier) {
        return couriers.findById(courier.id()).orElseThrow().getStatus();
    }

    private void setStatus(Account courier, CourierStatus status) {
        CourierProfile profile = couriers.findById(courier.id()).orElseThrow();
        profile.setStatus(status);
        couriers.saveAndFlush(profile);
    }

    private String deliveryUrl(Long orderId) {
        return "/api/courier/orders/" + orderId + "/delivery";
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void assignedCourierDeliversAPickedUpOrder() throws Exception {
        Account courier = register("delivery-happy@example.com", Role.COURIER);
        Order order = orderFor(courier, restaurant(), OrderStatus.PICKED_UP);
        setStatus(courier, CourierStatus.CARRYING);
        try {
            mockMvc.perform(post(deliveryUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(order.getId()))
                    .andExpect(jsonPath("$.orderStatus").value("delivered"))
                    .andExpect(jsonPath("$.courierStatus").value("idle"));

            Order saved = orders.findById(order.getId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(saved.getDateDelivered()).isNotNull();
            assertThat(statusOf(courier)).isEqualTo(CourierStatus.IDLE);
            assertThat(orderEvents.findAll()).anySatisfy(event -> {
                assertThat(event.getOrderId()).isEqualTo(order.getId());
                assertThat(event.getEventType()).isEqualTo("order_delivered");
                assertThat(event.getPreviousStatus()).isEqualTo("picked_up");
                assertThat(event.getNewStatus()).isEqualTo("delivered");
            });
        } finally {
            if (statusOf(courier) == CourierStatus.IDLE) courierDutyService.goOffDuty(courier.id());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void freedCourierIsOfferedTheOrderWaitingInTheDispatchQueue() throws Exception {
        Account courier = register("delivery-retry@example.com", Role.COURIER);
        Restaurant restaurant = restaurant();
        Order delivering = orderFor(courier, restaurant, OrderStatus.PICKED_UP);
        Order waiting = orders.saveAndFlush(new Order(restaurant.getId(), new Date(), OrderStatus.READY));
        orderEvents.saveAndFlush(new OrderEvent(waiting.getId(), "preparing", "ready", "order_ready",
                Instant.now().minus(Duration.ofDays(365))));
        setStatus(courier, CourierStatus.CARRYING);
        try {
            mockMvc.perform(post(deliveryUrl(delivering.getId()))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                    .andExpect(status().isOk());

            assertThat(orders.findById(waiting.getId()).orElseThrow().getCourierId()).isEqualTo(courier.id());
            assertThat(statusOf(courier)).isEqualTo(CourierStatus.ASSIGNED);
        } finally {
            orders.findById(waiting.getId()).filter(o -> o.getStatus() == OrderStatus.READY)
                    .ifPresent(o -> orderService.refundOrder(restaurant.getId(), waiting.getId()));
            if (statusOf(courier) == CourierStatus.IDLE) courierDutyService.goOffDuty(courier.id());
        }
    }

    @Test
    void refusesAnOrderThatIsNotPickedUp() throws Exception {
        Account courier = register("delivery-notpicked@example.com", Role.COURIER);
        Order order = orderFor(courier, restaurant(), OrderStatus.READY);

        mockMvc.perform(post(deliveryUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isConflict());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void refusesACourierWhoIsNotAssignedToTheOrder() throws Exception {
        Account owner = register("delivery-owner@example.com", Role.COURIER);
        Account other = register("delivery-other@example.com", Role.COURIER);
        Order order = orderFor(owner, restaurant(), OrderStatus.PICKED_UP);

        mockMvc.perform(post(deliveryUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isForbidden());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    @Test
    void refusesANonCourierRole() throws Exception {
        Account courier = register("delivery-assigned@example.com", Role.COURIER);
        Account customer = register("delivery-customer@example.com", Role.CUSTOMER);
        Order order = orderFor(courier, restaurant(), OrderStatus.PICKED_UP);

        mockMvc.perform(post(deliveryUrl(order.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer " + customer.token()))
                .andExpect(status().isForbidden());

        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    @Test
    void refusesAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(post(deliveryUrl(1L))).andExpect(status().isUnauthorized());
    }

    @Test
    void refusesAnUnknownOrder() throws Exception {
        Account courier = register("delivery-unknown@example.com", Role.COURIER);

        mockMvc.perform(post(deliveryUrl(999_999L)).header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isNotFound());
    }
}