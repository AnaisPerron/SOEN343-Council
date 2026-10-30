package com.council.tenderapp.ord.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.util.ReflectionTestUtils;

import com.council.tenderapp.ord.dtos.CustomerOrderDto;
import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderLine;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;

@ExtendWith(MockitoExtension.class)
class CustomerOrderHistoryServiceTest {

    private static final Long CUSTOMER_ID = 21L;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private JdbcTemplate jdbc;

    private CustomerOrderHistoryService service;

    @BeforeEach
    void setUp() {
        service = new CustomerOrderHistoryService(orderRepository, restaurantRepository, jdbc);
    }

    private static OrderLine line(Long id, String name, String price, int quantity) {
        MenuItem item = new MenuItem(id, 1L, new BigDecimal(price), "available", null, name, null, null);
        OrderLine line = new OrderLine(item, quantity, new BigDecimal(price));
        ReflectionTestUtils.setField(line, "id", id);
        return line;
    }

    @Test
    void listsOrdersWithRestaurantNamesAndAddOnPrices() {
        Order order = new Order(CUSTOMER_ID, 3L, OrderStatus.ACCEPTED, new BigDecimal("41.00"));
        order.setId(42L);
        order.addOrderLine(line(100L, "Pepperoni Pizza", "17.00", 2));
        order.addOrderLine(line(101L, "Sparkling Water", "3.00", 1));
        when(orderRepository.findByCustomerIdOrderByDateOrderedDescIdDesc(CUSTOMER_ID)).thenReturn(List.of(order));
        Restaurant restaurant = new Restaurant("Café Nord", "closed", null, null);
        restaurant.setId(3L);
        when(restaurantRepository.findAllById(List.of(3L))).thenReturn(List.of(restaurant));
        // Line 100 had "Extra pepperoni" (+3.00) selected.
        doAnswer(inv -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong(1)).thenReturn(100L);
            when(rs.getString(2)).thenReturn("Extra pepperoni");
            when(rs.getBigDecimal(3)).thenReturn(new BigDecimal("3.00"));
            inv.<RowCallbackHandler>getArgument(1).processRow(rs);
            return null;
        }).when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

        List<CustomerOrderDto> orders = service.getOrders(CUSTOMER_ID);

        assertThat(orders).singleElement().satisfies(dto -> {
            assertThat(dto.orderId()).isEqualTo(42L);
            assertThat(dto.restaurantName()).isEqualTo("Café Nord");
            assertThat(dto.status()).isEqualTo("accepted");
            assertThat(dto.total()).isEqualByComparingTo("43.00");
            assertThat(dto.lines().get(0).unitPrice()).isEqualByComparingTo("20.00");
            assertThat(dto.lines().get(0).options()).containsExactly("Extra pepperoni");
            assertThat(dto.lines().get(1).options()).isEmpty();
        });
    }

    @Test
    void customerWithoutOrdersGetsEmptyList() {
        when(orderRepository.findByCustomerIdOrderByDateOrderedDescIdDesc(CUSTOMER_ID)).thenReturn(List.of());

        assertThat(service.getOrders(CUSTOMER_ID)).isEmpty();
        verifyNoInteractions(restaurantRepository, jdbc);
    }
}
