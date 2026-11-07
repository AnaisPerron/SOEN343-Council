package com.council.tenderapp.ord.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.dtos.CustomerOrderDto;
import com.council.tenderapp.ord.dtos.CustomerOrderLineDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderLine;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;

@Service
public class CustomerOrderHistoryService {
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final JdbcTemplate jdbc;

    public CustomerOrderHistoryService(OrderRepository orderRepository, RestaurantRepository restaurantRepository,
            JdbcTemplate jdbc) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.jdbc = jdbc;
    }

    private record SelectedOption(String name, BigDecimal cost) {}

    @Transactional(readOnly = true)
    public List<CustomerOrderDto> getOrders(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerIdOrderByDateOrderedDescIdDesc(customerId);
        if (orders.isEmpty()) {
            return List.of();
        }

        // Restaurants may be closed now, so names come from the restaurant table rather than the browse list.
        Map<Long, String> restaurantNames = restaurantRepository
                .findAllById(orders.stream().map(Order::getRestaurantId).distinct().toList())
                .stream().collect(Collectors.toMap(Restaurant::getId, Restaurant::getName, (a, b) -> a));
        List<Long> lineIds = orders.stream().flatMap(order -> order.getOrderLines().stream()).map(OrderLine::getId).toList();
        Map<Long, List<SelectedOption>> optionsByLine = selectedOptions(lineIds);

        return orders.stream().map(order -> {
            List<CustomerOrderLineDto> lines = order.getOrderLines().stream().map(line -> {
                List<SelectedOption> options = optionsByLine.getOrDefault(line.getId(), List.of());
                BigDecimal unitPrice = options.stream().map(SelectedOption::cost).reduce(line.getUnitPrice(), BigDecimal::add);
                return new CustomerOrderLineDto(line.getMenuItem().getName(), line.getQuantity(), unitPrice,
                        options.stream().map(SelectedOption::name).toList());
            }).toList();
            // Summing the lines with their add-ons matches the charged total and also covers seeded orders without one.
            BigDecimal total = lines.stream().map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CustomerOrderDto(order.getId(), order.getRestaurantId(),
                    restaurantNames.getOrDefault(order.getRestaurantId(), "Restaurant"),
                    order.getStatus().name().toLowerCase(), total,
                    order.getDateOrdered() == null ? null : order.getDateOrdered().toInstant(),
                    order.getRejectionReason(), lines);
        }).toList();
    }

    // order_line_options has no JPA entity, so read it with JDBC.
    private Map<Long, List<SelectedOption>> selectedOptions(List<Long> lineIds) {
        if (lineIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(lineIds.size(), "?"));
        Map<Long, List<SelectedOption>> byLine = new HashMap<>();
        jdbc.query("SELECT olo.order_line_id, mio.name, olo.additional_cost FROM order_line_options olo "
                + "JOIN menu_item_options mio ON mio.id = olo.menu_item_option_id "
                + "WHERE olo.order_line_id IN (" + placeholders + ") ORDER BY mio.id",
                rs -> {
                    byLine.computeIfAbsent(rs.getLong(1), id -> new ArrayList<>())
                            .add(new SelectedOption(rs.getString(2), rs.getBigDecimal(3)));
                },
                lineIds.toArray());
        return byLine;
    }
}
