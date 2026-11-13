package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.dlv.services.DispatchService;
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
class CourierDutyControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RegistrationService registrationService;
    @Autowired private LoginService loginService;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private OrderRepository orders;
    @Autowired private DispatchService dispatchService;
    @Autowired private OrderService orderService;

    private record Account(Long id, String token) {}

    private Account register(String email, Role role) {
        Long id = registrationService.register(new RegisterRequest("Duty Tester", email, "secret123", "514-555-0170", role)).id();
        return new Account(id, loginService.login(new LoginRequest(email, "secret123")).token());
    }

    private Order orderFor(Account courier, Restaurant restaurant, OrderStatus status) {
        Order order = new Order(restaurant.getId(), new java.util.Date(), status);
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

    @Test
    void courierGoesOnDutyThenOffDuty() throws Exception {
        Account courier = register("duty-on-off@example.com", Role.COURIER);

        mockMvc.perform(post("/api/courier/duty/on").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courier.id()))
                .andExpect(jsonPath("$.status").value("idle"));
        assertThat(statusOf(courier)).isEqualTo(CourierStatus.IDLE);

        mockMvc.perform(post("/api/courier/duty/off").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("offline"));
        assertThat(statusOf(courier)).isEqualTo(CourierStatus.OFFLINE);
    }

    @Test
    void goingOffDutyMidDeliveryIsRefused() throws Exception {
        Account courier = register("duty-busy@example.com", Role.COURIER);

        for (CourierStatus busy : new CourierStatus[] {CourierStatus.ASSIGNED, CourierStatus.CARRYING}) {
            setStatus(courier, busy);
            mockMvc.perform(post("/api/courier/duty/off").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                    .andExpect(status().isConflict());
            assertThat(statusOf(courier)).isEqualTo(busy);
        }
    }

    @Test
    void onlyLoggedInCouriersCanChangeDutyStatus() throws Exception {
        Account staff = register("duty-staff@example.com", Role.STAFF);

        mockMvc.perform(post("/api/courier/duty/on")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/courier/duty/on").header(HttpHeaders.AUTHORIZATION, "Bearer " + staff.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/courier/duty/off").header(HttpHeaders.AUTHORIZATION, "Bearer " + staff.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void showsCurrentStateWithNoOrderForANewCourier() throws Exception {
        Account courier = register("duty-get@example.com", Role.COURIER);

        mockMvc.perform(get("/api/courier/duty").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("offline"))
                .andExpect(jsonPath("$.assignedOrder").doesNotExist());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void goingOnDutyReceivesAnOfferAndMustAcceptBeforePickup() throws Exception {
        Account courier = register("duty-dispatch@example.com", Role.COURIER);
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Queue Cafe", "open", 45.50, -73.57));
        Order waiting = orders.saveAndFlush(new Order(restaurant.getId(), new java.util.Date(), OrderStatus.READY));

        dispatchService.retryQueue();
        assertThat(orders.findById(waiting.getId()).orElseThrow().getCourierId()).isNull();
        assertThat(orders.findWaitingReadyOrdersForDispatch())
                .extracting(Order::getId)
                .contains(waiting.getId());

        mockMvc.perform(post("/api/courier/duty/on").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("idle"))
                .andExpect(jsonPath("$.assignedOrder.orderId").value(waiting.getId()));

        assertThat(orders.findById(waiting.getId()).orElseThrow().getCourierId()).isEqualTo(courier.id());
        assertThat(statusOf(courier)).isEqualTo(CourierStatus.IDLE);

        mockMvc.perform(get("/api/courier/duty").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("idle"))
                .andExpect(jsonPath("$.assignedOrder.orderId").value(waiting.getId()))
                .andExpect(jsonPath("$.assignedOrder.status").value("ready"))
                .andExpect(jsonPath("$.assignedOrder.restaurantName").value("Queue Cafe"));

        mockMvc.perform(post("/api/courier/duty/orders/" + waiting.getId() + "/picked-up")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isConflict());
        assertThat(orders.findById(waiting.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);

        mockMvc.perform(post("/api/courier/duty/orders/" + waiting.getId() + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("assigned"));
        assertThat(statusOf(courier)).isEqualTo(CourierStatus.ASSIGNED);

        mockMvc.perform(post("/api/courier/duty/orders/" + waiting.getId() + "/picked-up")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("carrying"))
                .andExpect(jsonPath("$.assignedOrder.status").value("picked_up"));
        assertThat(orders.findById(waiting.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        assertThat(statusOf(courier)).isEqualTo(CourierStatus.CARRYING);

        mockMvc.perform(post("/api/courier/duty/orders/" + waiting.getId() + "/delivered")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("idle"));
        mockMvc.perform(post("/api/courier/duty/off").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk());
    }

    @Test
    void decliningAnOfferReturnsItToTheQueueAndTakesCourierOffDuty() throws Exception {
        Account courier = register("duty-decline@example.com", Role.COURIER);
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Decline Cafe", "open", 45.50, -73.57));
        Order waiting = orders.saveAndFlush(new Order(restaurant.getId(), new java.util.Date(), OrderStatus.READY));
        mockMvc.perform(post("/api/courier/duty/on").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk());
        Order offered = orders.findById(waiting.getId()).orElseThrow();
        assertThat(offered.getCourierId()).isEqualTo(courier.id());

        mockMvc.perform(post("/api/courier/duty/orders/" + waiting.getId() + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("offline"))
                .andExpect(jsonPath("$.assignedOrder").doesNotExist());

        assertThat(statusOf(courier)).isEqualTo(CourierStatus.OFFLINE);
        assertThat(orders.findById(waiting.getId()).orElseThrow().getCourierId()).isNull();
        assertThat(orders.findWaitingReadyOrdersForDispatch()).extracting(Order::getId).contains(waiting.getId());
        orderService.refundOrder(restaurant.getId(), waiting.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dutyToggleWithdrawsAnOutstandingOffer() throws Exception {
        Account courier = register("duty-toggle-offer@example.com", Role.COURIER);
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Toggle Cafe", "open", 45.50, -73.57));
        Order waiting = orders.saveAndFlush(new Order(restaurant.getId(), new java.util.Date(), OrderStatus.READY));

        mockMvc.perform(post("/api/courier/duty/on").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedOrder.orderId").value(waiting.getId()));

        mockMvc.perform(post("/api/courier/duty/off").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("offline"))
                .andExpect(jsonPath("$.assignedOrder").doesNotExist());

        assertThat(statusOf(courier)).isEqualTo(CourierStatus.OFFLINE);
        assertThat(orders.findById(waiting.getId()).orElseThrow().getCourierId()).isNull();
        assertThat(orders.findWaitingReadyOrdersForDispatch()).extracting(Order::getId).contains(waiting.getId());
        orderService.refundOrder(restaurant.getId(), waiting.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void courierCanMarkTheirPickedUpOrderDeliveredAndReturnsToIdle() throws Exception {
        Account courier = register("duty-delivered@example.com", Role.COURIER);
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Delivery Cafe", "open", 45.50, -73.57));
        Order pickedUp = new Order(restaurant.getId(), new java.util.Date(), OrderStatus.PICKED_UP);
        pickedUp.setCourierId(courier.id());
        pickedUp = orders.saveAndFlush(pickedUp);
        setStatus(courier, CourierStatus.ASSIGNED);

        mockMvc.perform(post("/api/courier/duty/orders/" + pickedUp.getId() + "/delivered")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("idle"));

        assertThat(orders.findById(pickedUp.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(statusOf(courier)).isEqualTo(CourierStatus.IDLE);
        mockMvc.perform(post("/api/courier/duty/off").header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                        .andExpect(status().isOk());
    }
     @Test
    void deliveryBeforePickupIsRefused() throws Exception {
        Account courier = register("duty-early-delivery@example.com", Role.COURIER);
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Early Cafe", "open", 45.50, -73.57));
        Order ready = orderFor(courier, restaurant, OrderStatus.READY);
        setStatus(courier, CourierStatus.ASSIGNED);

        mockMvc.perform(post("/api/courier/duty/orders/" + ready.getId() + "/delivered")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + courier.token()))
                .andExpect(status().isBadRequest());

        assertThat(orders.findById(ready.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(statusOf(courier)).isEqualTo(CourierStatus.ASSIGNED);
    }

    @Test
    void anotherCourierCannotConfirmPickup() throws Exception {
        Account owner = register("duty-pickup-owner@example.com", Role.COURIER);
        Account other = register("duty-pickup-other@example.com", Role.COURIER);
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Owner Cafe", "open", 45.50, -73.57));
        Order ownersOrder = orderFor(owner, restaurant, OrderStatus.READY);
        Order othersOrder = orderFor(other, restaurant, OrderStatus.READY);
        setStatus(owner, CourierStatus.ASSIGNED);
        setStatus(other, CourierStatus.ASSIGNED);

        mockMvc.perform(post("/api/courier/duty/orders/" + ownersOrder.getId() + "/picked-up")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isConflict());

        assertThat(orders.findById(ownersOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(orders.findById(othersOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(statusOf(owner)).isEqualTo(CourierStatus.ASSIGNED);
        assertThat(statusOf(other)).isEqualTo(CourierStatus.ASSIGNED);
    }
}
