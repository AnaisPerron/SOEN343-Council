package com.council.tenderapp.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.operator.dto.RestaurantStatus;
import com.council.tenderapp.operator.services.OperatorRestaurantService;
import com.council.tenderapp.ord.dtos.OrderItemRequestDto;
import com.council.tenderapp.ord.dtos.PlaceOrderRequestDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.exceptions.RestaurantClosedException;
import com.council.tenderapp.ord.exceptions.RestaurantNotFoundException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.ord.services.BrowseService;
import com.council.tenderapp.ord.services.CustomerOrderService;

@SpringBootTest
@Transactional
class OperatorRestaurantIntegrationTest {
    @Autowired private OperatorRestaurantService service;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private OrderRepository orders;
    @Autowired private BrowseService browse;
    @Autowired private CustomerOrderService customerOrders;

    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        authenticate("OPERATOR");
        restaurant = new Restaurant("Status test", "open", 45.5, -73.5);
        restaurant.setAddress("Main Street");
        restaurant.setPhone("5145550100");
        restaurant = restaurants.saveAndFlush(restaurant);
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                8L, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void pauseAndReopenPersistWithoutChangingOtherDetailsOrExistingOrders() {
        Order order = orders.saveAndFlush(new Order(restaurant.getId(), new Date(), OrderStatus.ACCEPTED));
        long count = orders.count();
        for (RestaurantStatus state : List.of(RestaurantStatus.PAUSED, RestaurantStatus.OPEN)) {
            service.updateStatus(restaurant.getId(), state);
            Restaurant updated = restaurants.findById(restaurant.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(state.value());
            assertThat(updated.getName()).isEqualTo("Status test");
            assertThat(updated.getAddress()).isEqualTo("Main Street");
            assertThat(updated.getPhone()).isEqualTo("5145550100");
            assertThat(updated.getLatitude()).isEqualTo(45.5);
            assertThat(orders.count()).isEqualTo(count);
            assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }
    }

    @ParameterizedTest
    @EnumSource(value = RestaurantStatus.class, names = {"PAUSED", "CLOSED"})
    void nonOpenRestaurantsRejectNewOrdersThroughTheExistingService(RestaurantStatus state) {
        var request = new PlaceOrderRequestDto(restaurant.getId(), List.of(new OrderItemRequestDto(999L, 1)));
        long count = orders.count();
        service.updateStatus(restaurant.getId(), state);
        assertThatThrownBy(() -> customerOrders.placeOrder(1L, request)).isInstanceOf(RestaurantClosedException.class);
        assertThat(orders.count()).isEqualTo(count);
    }

    @Test
    void closedRestaurantFailsTheOrderingCheckAndReopenedRestaurantPasses() {
        service.updateStatus(restaurant.getId(), RestaurantStatus.CLOSED);
        assertThatThrownBy(() -> browse.checkRestaurantOpen(restaurant.getId())).isInstanceOf(RestaurantClosedException.class);
        service.updateStatus(restaurant.getId(), RestaurantStatus.OPEN);
        assertThatCode(() -> browse.checkRestaurantOpen(restaurant.getId())).doesNotThrowAnyException();
    }

    @Test
    void repeatedUpdatesAreIdempotentAndOtherRestaurantsAreUnaffected() {
        Restaurant other = restaurants.saveAndFlush(new Restaurant("Other", "open", null, null));
        service.updateStatus(restaurant.getId(), RestaurantStatus.PAUSED);
        service.updateStatus(restaurant.getId(), RestaurantStatus.PAUSED);
        assertThat(restaurants.findById(restaurant.getId()).orElseThrow().getStatus()).isEqualTo("paused");
        assertThat(restaurants.findById(other.getId()).orElseThrow().getStatus()).isEqualTo("open");
    }

    @Test
    void unknownRestaurantDoesNotCreateOrChangeRecords() {
        long count = restaurants.count();
        assertThatThrownBy(() -> service.updateStatus(Long.MAX_VALUE, RestaurantStatus.PAUSED))
                .isInstanceOf(RestaurantNotFoundException.class);
        assertThat(restaurants.count()).isEqualTo(count);
        assertThat(restaurants.findById(restaurant.getId()).orElseThrow().getStatus()).isEqualTo("open");
    }

    @Test
    void serviceAlsoRejectsNonOperatorCallers() {
        authenticate("CUSTOMER");
        assertThatThrownBy(() -> service.updateStatus(restaurant.getId(), RestaurantStatus.PAUSED))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(restaurants.findById(restaurant.getId()).orElseThrow().getStatus()).isEqualTo("open");
    }
}
