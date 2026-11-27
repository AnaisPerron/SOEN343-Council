package com.council.tenderapp.rvw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderStateException;
import com.council.tenderapp.ord.exceptions.OrderAccessDeniedException;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.reg.entities.Role;
import com.council.tenderapp.rvw.dtos.OrderRatingResponseDto;
import com.council.tenderapp.rvw.dtos.SubmitRatingRequestDto;
import com.council.tenderapp.rvw.exceptions.InvalidRatingException;
import com.council.tenderapp.rvw.exceptions.OrderAlreadyRatedException;
import com.council.tenderapp.rvw.repositories.OrderRatingRepository;
import com.council.tenderapp.rvw.services.OrderRatingService;

@SpringBootTest
class OrderRatingServiceIntegrationTest {
    private static final long CUSTOMER = 1L;
    private static final long RESTAURANT = 901L;
    private static final long COURIER = 6L;

    @Autowired private OrderRatingService service;
    @Autowired private OrderRatingRepository ratings;
    @Autowired private OrderRepository orders;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void authenticateCustomer() {
        authenticate("CUSTOMER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Long id : created) {
            jdbc.update("DELETE FROM order_ratings WHERE order_id = ?", id);
            orders.deleteById(id);
        }
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CUSTOMER, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Long order(OrderStatus status, Long courierId) {
        Long id = orders.saveAndFlush(new Order(CUSTOMER, RESTAURANT, OrderStatus.PLACED, new BigDecimal("20.00"))).getId();
        jdbc.update("UPDATE orders SET status = ?, courier_id = ? WHERE id = ?",
                status.name().toLowerCase(Locale.ROOT), courierId, id);
        created.add(id);
        return id;
    }

    private long ratingCount(Long orderId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM order_ratings WHERE order_id = ?", Long.class, orderId);
    }

    @Test
    void customerRatesTheirDeliveredOrderAndIdsAreCopiedFromTheOrder() {
        Long orderId = order(OrderStatus.DELIVERED, COURIER);

        OrderRatingResponseDto response = service.rate(CUSTOMER, orderId,
                new SubmitRatingRequestDto(5, 4, "  Hot and on time  "));

        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.restaurantId()).isEqualTo(RESTAURANT);
        assertThat(response.courierId()).isEqualTo(COURIER);
        assertThat(response.restaurantStars()).isEqualTo(5);
        assertThat(response.courierStars()).isEqualTo(4);
        assertThat(response.comment()).isEqualTo("Hot and on time");
        assertThat(response.createdAt()).isNotNull();
        var stored = ratings.findById(response.id()).orElseThrow();
        assertThat(stored.getCustomerId()).isEqualTo(CUSTOMER);
        assertThat(stored.getCourierId()).isEqualTo(COURIER);
    }

    @Test
    void restaurantAndCourierSeeTheirOwnStarsAndSharedCommentOnly() {
        Long orderId = order(OrderStatus.DELIVERED, COURIER);
        service.rate(CUSTOMER, orderId, new SubmitRatingRequestDto(5, 4, "Hot and on time"));

        authenticate("STAFF");
        var restaurantRating = service.view(orderId, Role.STAFF, 7L, RESTAURANT);
        assertThat(restaurantRating.stars()).isEqualTo(5);
        assertThat(restaurantRating.comment()).isEqualTo("Hot and on time");

        authenticate("COURIER");
        var courierRating = service.view(orderId, Role.COURIER, COURIER, null);
        assertThat(courierRating.stars()).isEqualTo(4);
        assertThat(courierRating.restaurantStars()).isNull();
        assertThat(courierRating.comment()).isEqualTo("Hot and on time");
        assertThatThrownBy(() -> service.view(orderId, Role.COURIER, COURIER + 1, null))
                .isInstanceOf(com.council.tenderapp.prc.exceptions.UnauthorizedOrderAccessException.class);
        assertThatThrownBy(() -> service.view(orderId, Role.STAFF, 7L, RESTAURANT + 1))
                .isInstanceOf(com.council.tenderapp.prc.exceptions.UnauthorizedOrderAccessException.class);

        authenticate("CUSTOMER");
        var customerRating = service.view(orderId, Role.CUSTOMER, CUSTOMER, null);
        assertThat(customerRating.stars()).isNull();
        assertThat(customerRating.restaurantStars()).isEqualTo(5);
        assertThat(customerRating.courierStars()).isEqualTo(4);
        assertThat(customerRating.comment()).isEqualTo("Hot and on time");
        assertThatThrownBy(() -> service.view(orderId, Role.CUSTOMER, CUSTOMER + 1, null))
                .isInstanceOf(com.council.tenderapp.prc.exceptions.UnauthorizedOrderAccessException.class);
    }

    @Test
    void authorisedHistoryOrderWithoutARatingReturnsNull() {
        Long orderId = order(OrderStatus.DELIVERED, COURIER);
        authenticate("COURIER");
        assertThat(service.view(orderId, Role.COURIER, COURIER, null)).isNull();
    }

    @Test
    void secondRatingOfTheSameOrderIsRefused() {
        Long orderId = order(OrderStatus.DELIVERED, COURIER);
        service.rate(CUSTOMER, orderId, new SubmitRatingRequestDto(4, 4, null));

        assertThatThrownBy(() -> service.rate(CUSTOMER, orderId, new SubmitRatingRequestDto(1, 1, "changed my mind")))
                .isInstanceOf(OrderAlreadyRatedException.class)
                .hasMessage("Order " + orderId + " has already been rated");
        assertThat(ratingCount(orderId)).isEqualTo(1);
        assertThat(ratings.findAll().stream().filter(r -> r.getOrderId().equals(orderId)).findFirst().orElseThrow()
                .getRestaurantStars()).isEqualTo(4);
    }

    @Test
    void anotherCustomersOrderIsForbiddenAndMissingOrderIsNotFound() {
        Long orderId = order(OrderStatus.DELIVERED, COURIER);
        assertThatThrownBy(() -> service.rate(2L, orderId, new SubmitRatingRequestDto(5, 5, null)))
                .isInstanceOf(OrderAccessDeniedException.class);
        assertThatThrownBy(() -> service.rate(CUSTOMER, Long.MAX_VALUE, new SubmitRatingRequestDto(5, 5, null)))
                .isInstanceOf(OrderNotFoundException.class);
        assertThat(ratingCount(orderId)).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DELIVERED")
    void onlyDeliveredOrdersCanBeRated(OrderStatus status) {
        Long orderId = order(status, COURIER);
        assertThatThrownBy(() -> service.rate(CUSTOMER, orderId, new SubmitRatingRequestDto(5, 5, null)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining(status.name().toLowerCase(Locale.ROOT));
        assertThat(ratingCount(orderId)).isZero();
    }

    @Test
    void courierStarsAreRequiredExactlyWhenACourierDelivered() {
        Long withCourier = order(OrderStatus.DELIVERED, COURIER);
        assertThatThrownBy(() -> service.rate(CUSTOMER, withCourier, new SubmitRatingRequestDto(5, null, null)))
                .isInstanceOf(InvalidRatingException.class);

        Long withoutCourier = order(OrderStatus.DELIVERED, null);
        assertThatThrownBy(() -> service.rate(CUSTOMER, withoutCourier, new SubmitRatingRequestDto(5, 3, null)))
                .isInstanceOf(InvalidRatingException.class);
        var response = service.rate(CUSTOMER, withoutCourier, new SubmitRatingRequestDto(3, null, "   "));
        assertThat(response.courierId()).isNull();
        assertThat(response.courierStars()).isNull();
        assertThat(response.comment()).isNull();
    }

    @Test
    void nonCustomerRolesCannotCallTheService() {
        Long orderId = order(OrderStatus.DELIVERED, COURIER);
        for (String role : List.of("COURIER", "STAFF", "OPERATOR")) {
            authenticate(role);
            assertThatThrownBy(() -> service.rate(CUSTOMER, orderId, new SubmitRatingRequestDto(5, 5, null)))
                    .isInstanceOf(AccessDeniedException.class);
        }
        assertThat(ratingCount(orderId)).isZero();
    }

    @Test
    void concurrentRatingsOfOneOrderStoreExactlyOne() throws Exception {
        Long orderId = order(OrderStatus.DELIVERED, COURIER);
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            authenticate("CUSTOMER");
            try {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                service.rate(CUSTOMER, orderId, new SubmitRatingRequestDto(5, 5, null));
                return "rated";
            } catch (OrderAlreadyRatedException duplicate) {
                return "duplicate";
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("rated", "duplicate");
            assertThat(ratingCount(orderId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
