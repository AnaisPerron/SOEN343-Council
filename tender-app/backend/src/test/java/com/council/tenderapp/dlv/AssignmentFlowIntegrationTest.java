package com.council.tenderapp.dlv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.council.tenderapp.dlv.entities.Assignment;
import com.council.tenderapp.dlv.entities.AssignmentStatus;
import com.council.tenderapp.dlv.repositories.AssignmentRepository;
import com.council.tenderapp.dlv.services.AssignmentService;
import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.dlv.services.CourierOfferService;
import com.council.tenderapp.operator.services.OperatorReassignmentService;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.reg.entities.CourierProfile;
import com.council.tenderapp.reg.entities.CourierStatus;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.reg.dtos.RegisterRequest;
import com.council.tenderapp.reg.repositories.ProfileRepository;
import com.council.tenderapp.reg.services.RegistrationService;
import com.council.tenderapp.reg.repositories.CourierProfileRepository;
import com.council.tenderapp.reg.services.SessionService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:assignment-flows;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class AssignmentFlowIntegrationTest {
    @Autowired private OrderService orderService;
    @Autowired private CourierOfferService offers;
    @Autowired private CourierDutyService duty;
    @Autowired private OperatorReassignmentService reassignments;
    @Autowired private OrderRepository orders;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private CourierProfileRepository couriers;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private SessionService sessions;
    @Autowired private RegistrationService registration;
    @Autowired private ProfileRepository profiles;
    @Autowired private MockMvc mvc;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoSpyBean private AssignmentRepository assignments;
    private Restaurant restaurant;
    private Order order;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant("Offer Cafe", "open", 45.5, -73.5);
        restaurant.setAddress("123 Pickup Street");
        restaurant = restaurants.saveAndFlush(restaurant);
        courier(11, CourierStatus.IDLE, 45.501, -73.501);
        courier(12, CourierStatus.IDLE, 45.8, -73.8);
        order = preparingOrder();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        assignments.deleteAllInBatch();
        orders.deleteAllInBatch();
        couriers.deleteAllInBatch();
        restaurants.deleteAllInBatch();
        profiles.deleteAllInBatch();
    }

    private Order preparingOrder() {
        var order = new Order(restaurant.getId(), new Date(), OrderStatus.PREPARING);
        order.setDelivery("456 Delivery Street", 45.51, -73.51);
        return orders.saveAndFlush(order);
    }

    private void courier(long id, CourierStatus status, double lat, double lon) {
        var courier = new CourierProfile(id);
        courier.setStatus(status);
        courier.setLatitude(lat);
        courier.setLongitude(lon);
        couriers.saveAndFlush(courier);
    }

    private void authenticate(long id, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Assignment active(long orderId) {
        return assignments.findByOrderIdAndStatusIn(orderId, AssignmentService.ACTIVE).orElseThrow();
    }

    private long readyOffer() {
        orderService.markReady(restaurant.getId(), order.getId());
        return active(order.getId()).getId();
    }

    @Test
    void markingReadyCreatesAnOfferAndReservesNearestCourier() {
        readyOffer();
        assertThat(active(order.getId()).getCourierId()).isEqualTo(11L);
        assertThat(active(order.getId()).getStatus()).isEqualTo(AssignmentStatus.OFFERED);
        assertThat(active(order.getId()).getAcceptedAt()).isNull();
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isEqualTo(11L);
        assertThat(couriers.findById(11L).orElseThrow().getStatus()).isEqualTo(CourierStatus.ASSIGNED);
        assertThat(couriers.findById(12L).orElseThrow().getStatus()).isEqualTo(CourierStatus.IDLE);
    }

    @Test
    void readyOrderWaitsUntilACourierComesOnDuty() {
        courier(11, CourierStatus.OFFLINE, 45.501, -73.501);
        courier(12, CourierStatus.CARRYING, 45.8, -73.8);
        orderService.markReady(restaurant.getId(), order.getId());
        assertThat(assignments.count()).isZero();
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isNull();
        assertThat(duty.goOnDuty(11L).assignedOrder().assignmentStatus()).isEqualTo("offered");
        assertThat(active(order.getId()).getCourierId()).isEqualTo(11L);
    }

    @Test
    void courierWithAnActiveAssignmentIsNotGivenASecondOrderEvenIfTheirStateIsIncorrect() {
        readyOffer();
        courier(11, CourierStatus.IDLE, 45.501, -73.501);
        var second = preparingOrder();
        orderService.markReady(restaurant.getId(), second.getId());
        assertThat(active(second.getId()).getCourierId()).isEqualTo(12L);
        assertThat(assignments.findAll().stream().filter(a -> a.getCourierId() == 11L && a.getStatus().isActive()).count()).isEqualTo(1);
    }

    @Test
    void acceptanceIsOwnedIdempotentAndRequiredBeforePickup() {
        long id = readyOffer();
        assertThatThrownBy(() -> orderService.markPickedUpByCourier(11L, order.getId()))
                .isInstanceOf(InvalidOrderStateException.class);
        authenticate(12, "COURIER");
        assertThatThrownBy(() -> offers.accept(12, id)).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(403));
        authenticate(11, "COURIER");
        var accepted = offers.accept(11, id);
        assertThat(accepted.acceptedAt()).isNotNull();
        assertThat(offers.accept(11, id).acceptedAt()).isEqualTo(accepted.acceptedAt());
        assertThat(assignments.count()).isEqualTo(1);
        orderService.markPickedUpByCourier(11L, order.getId());
        assertThat(couriers.findById(11L).orElseThrow().getStatus()).isEqualTo(CourierStatus.CARRYING);
        orderService.markDeliveredByCourier(11L, order.getId());
        assertThat(assignments.findById(id).orElseThrow().getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(couriers.findById(11L).orElseThrow().getStatus()).isEqualTo(CourierStatus.IDLE);
        assertThat(duty.goOffDuty(11L).status()).isEqualTo("offline");
    }

    @Test
    void reassignmentEndsOldOfferAndCreatesANewOfferWithoutDeletingHistory() {
        long old = readyOffer();
        authenticate(8, "OPERATOR");
        reassignments.reassign(order.getId(), 11);
        assertThat(assignments.findById(old).orElseThrow().getStatus()).isEqualTo(AssignmentStatus.ENDED);
        assertThat(active(order.getId()).getCourierId()).isEqualTo(12L);
        assertThat(assignments.count()).isEqualTo(2);
        authenticate(11, "COURIER");
        assertThatThrownBy(() -> offers.accept(11, old)).isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void decliningAnOfferEndsItsAssignmentAndRedispatches() {
        long id = readyOffer();
        assertThat(duty.declineOffer(11L, order.getId()).status()).isEqualTo("offline");
        assertThat(assignments.findById(id).orElseThrow().getStatus()).isEqualTo(AssignmentStatus.ENDED);
        assertThat(active(order.getId()).getCourierId()).isEqualTo(12L);
    }

    @Test
    void acceptedOffersCannotBeDeclinedOrWithdrawnByGoingOffDuty() {
        long id = readyOffer();
        authenticate(11, "COURIER");
        offers.accept(11, id);
        assertThatThrownBy(() -> duty.declineOffer(11L, order.getId()))
                .isInstanceOf(com.council.tenderapp.dlv.exceptions.InvalidCourierStateException.class);
        assertThatThrownBy(() -> duty.goOffDuty(11L))
                .isInstanceOf(com.council.tenderapp.dlv.exceptions.InvalidCourierStateException.class);
        assertThat(active(order.getId()).getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
    }

    @Test
    void replacementCourierNameStaysHiddenUntilTheReplacementAccepts() {
        jdbc.update("INSERT INTO users (id,role,name,email,password_hash,phone) VALUES (11,'courier','Near Courier','near@test.test','hash','5551111111'),(12,'courier','Far Courier','far@test.test','hash','5552222222')");
        long id = readyOffer();
        authenticate(11, "COURIER");
        offers.accept(11, id);
        assertThat(orderService.getAllOrders(restaurant.getId()).stream().filter(o -> o.getId().equals(order.getId())).findFirst().orElseThrow().getCourierName()).isEqualTo("Near Courier");
        authenticate(8, "OPERATOR");
        reassignments.reassign(order.getId(), 11);
        assertThat(orderService.getAllOrders(restaurant.getId()).stream().filter(o -> o.getId().equals(order.getId())).findFirst().orElseThrow().getCourierName()).isNull();
        authenticate(12, "COURIER");
        offers.accept(12, active(order.getId()).getId());
        assertThat(orderService.getAllOrders(restaurant.getId()).stream().filter(o -> o.getId().equals(order.getId())).findFirst().orElseThrow().getCourierName()).isEqualTo("Far Courier");
    }

    @Test
    void refundEndsOfferAndReleasesCourierInTheSameOperation() {
        long id = readyOffer();
        Order delivered = orders.findById(order.getId()).orElseThrow();
        delivered.transitionTo(OrderStatus.PICKED_UP);
        delivered.transitionTo(OrderStatus.DELIVERED);
        orders.saveAndFlush(delivered);
        orderService.refundOrder(restaurant.getId(), order.getId());
        assertThat(assignments.findById(id).orElseThrow().getStatus()).isEqualTo(AssignmentStatus.ENDED);
        assertThat(couriers.findById(11L).orElseThrow().getStatus()).isEqualTo(CourierStatus.IDLE);
    }

    @Test
    void rollbackRestoresOrderCourierAndAssignmentTogether() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            orderService.markReady(restaurant.getId(), order.getId());
            assertThat(assignments.count()).isEqualTo(1);
            tx.setRollbackOnly();
        });
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(orders.findById(order.getId()).orElseThrow().getCourierId()).isNull();
        assertThat(couriers.findById(11L).orElseThrow().getStatus()).isEqualTo(CourierStatus.IDLE);
        assertThat(assignments.count()).isZero();
    }

    @Test
    void assignmentSaveFailureRollsBackTheReadyTransition() {
        doThrow(new DataIntegrityViolationException("assignment conflict")).when(assignments).saveAndFlush(any(Assignment.class));
        assertThatThrownBy(this::readyOffer).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(couriers.findById(11L).orElseThrow().getStatus()).isEqualTo(CourierStatus.IDLE);
        assertThat(assignments.count()).isZero();
    }

    @Test
    void concurrentReadyOrdersCannotReserveTheSameCourier() throws Exception {
        courier(12, CourierStatus.OFFLINE, 45.8, -73.8);
        var second = preparingOrder();
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> { start.await(); return orderService.markReady(restaurant.getId(), order.getId()); });
            var other = executor.submit(() -> { start.await(); return orderService.markReady(restaurant.getId(), second.getId()); });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            other.get(10, TimeUnit.SECONDS);
            assertThat(assignments.count()).isEqualTo(1);
            assertThat(List.of(orders.findById(order.getId()).orElseThrow(), orders.findById(second.getId()).orElseThrow())
                    .stream().filter(o -> o.getCourierId() != null).count()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void acceptanceEndpointShowsAddressesAndNoPaymentDetails() throws Exception {
        long id = readyOffer();
        String token = sessions.create(11L, Role.COURIER).token();
        mvc.perform(post("/api/courier/offers/" + id + "/accept").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignmentStatus").value("accepted"))
                .andExpect(jsonPath("$.restaurantAddress").value("123 Pickup Street"))
                .andExpect(jsonPath("$.deliveryAddress").value("456 Delivery Street"))
                .andExpect(jsonPath("$.totalPrice").doesNotExist())
                .andExpect(jsonPath("$.paymentMethod").doesNotExist())
                .andExpect(jsonPath("$.cardNumber").doesNotExist());
    }

    @Test
    void endpointRejectsOtherRolesInvalidIdsMissingOffersAndOtherCouriers() throws Exception {
        long id = readyOffer();
        mvc.perform(post("/api/courier/offers/" + id + "/accept")).andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.OPERATOR, Role.CUSTOMER, Role.STAFF)) {
            mvc.perform(post("/api/courier/offers/" + id + "/accept")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessions.create(8L, role).token()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/courier/offers/" + id + "/accept")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessions.create(12L, Role.COURIER).token()))
                .andExpect(status().isForbidden());
        String token = "Bearer " + sessions.create(11L, Role.COURIER).token();
        for (String invalid : List.of("0", "-1", "invalid")) {
            mvc.perform(post("/api/courier/offers/" + invalid + "/accept").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/courier/offers/999999/accept").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void accountDeletionRejectsActiveAssignmentsAndCleansUpEndedHistory() throws Exception {
        long courierId = registration.register(new RegisterRequest("Delete courier", "delete-courier@fixture.test",
                "LocalTest123!", "5145550199", Role.COURIER)).id();
        order = new Order(restaurant.getId(), new Date(), OrderStatus.READY);
        order.setCourierId(courierId);
        order = orders.saveAndFlush(order);
        Assignment assignment = assignments.saveAndFlush(new Assignment(order.getId(), courierId, java.time.Instant.now()));
        String token = "Bearer " + sessions.create(courierId, Role.COURIER).token();
        mvc.perform(delete("/api/profile").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isConflict());
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            assignments.findById(assignment.getId()).orElseThrow().end();
            Order delivered = orders.findById(order.getId()).orElseThrow();
            delivered.transitionTo(OrderStatus.PICKED_UP);
            delivered.transitionTo(OrderStatus.DELIVERED);
            delivered.transitionTo(OrderStatus.REFUNDED);
        });
        mvc.perform(delete("/api/profile").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNoContent());
        assertThat(assignments.findById(assignment.getId())).isEmpty();
        assertThat(profiles.findById(courierId)).isEmpty();
    }

    @Test
    void serviceCannotAcceptOnBehalfOfAnotherCourier() {
        long id = readyOffer();
        authenticate(12, "COURIER");
        assertThatThrownBy(() -> offers.accept(11, id)).isInstanceOf(AccessDeniedException.class);
        assertThat(active(order.getId()).getStatus()).isEqualTo(AssignmentStatus.OFFERED);
    }
}
