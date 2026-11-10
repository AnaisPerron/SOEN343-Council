package com.council.tenderapp.prc;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.council.tenderapp.ord.entities.MenuItem;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderLine;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.entities.Restaurant;
import com.council.tenderapp.ord.repositories.MenuItemRepository;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.repositories.RestaurantRepository;
import com.council.tenderapp.prc.dtos.response.SummaryDto;
import com.council.tenderapp.prc.service.SummaryService;

@SpringBootTest(properties = {
        "app.pricing.delivery-fee-per-km=0.80",
        "app.pricing.service-fee=1.50",
        "app.pricing.small-order-threshold=10.00",
        "app.pricing.small-order-surcharge=2.00"
})
class SummaryServiceIntegrationTest {
    @Autowired private SummaryService summaries;
    @Autowired private OrderRepository orders;
    @Autowired private OrderEventRepository events;
    @Autowired private RestaurantRepository restaurants;
    @Autowired private MenuItemRepository menuItems;

    private Long orderId;
    private Long eventId;
    private Long restaurantId;
    private Long menuItemId;

    @AfterEach
    void cleanUp() {
        if (eventId != null) events.deleteById(eventId);
        if (orderId != null) orders.deleteById(orderId);
        if (menuItemId != null) menuItems.deleteById(menuItemId);
        if (restaurantId != null) restaurants.deleteById(restaurantId);
    }

    @Test
    void inProgressOrderWithItemsAndNoLedgerReturnsSummary() {
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Summary Cafe", "open", 45.5, -73.6));
        restaurantId = restaurant.getId();
        MenuItem item = new MenuItem(null, "Coffee", new BigDecimal("4.00"));
        item.setMenuId(1L);
        item = menuItems.saveAndFlush(item);
        menuItemId = item.getId();
        Order order = new Order(null, restaurantId, OrderStatus.PLACED, new BigDecimal("8.00"));
        order.addOrderLine(new OrderLine(item, 2, new BigDecimal("4.00")));
        orderId = orders.saveAndFlush(order).getId();

        SummaryDto summary = summaries.generateOrderSummary(orderId);

        assertThat(summary.status()).isEqualTo("PLACED");
        assertThat(summary.restaurantName()).isEqualTo("Summary Cafe");
        assertThat(summary.orderItems()).hasSize(1);
        assertThat(summary.orderItems().get(0).name()).isEqualTo("Coffee");
        assertThat(summary.placedAt()).isNotNull();
        assertThat(summary.finishedAt()).isNull();
    }

    @Test
    void cancelledOrderIncludesCancellationEventTime() {
        Restaurant restaurant = restaurants.saveAndFlush(new Restaurant("Summary Cafe", "open", 45.5, -73.6));
        restaurantId = restaurant.getId();
        Order order = new Order(null, restaurantId, OrderStatus.CANCELLED, new BigDecimal("17.48"));
        orderId = orders.saveAndFlush(order).getId();
        Instant cancelledAt = Instant.parse("2026-11-10T18:30:00Z");
        eventId = events.saveAndFlush(new OrderEvent(orderId, "placed", "cancelled", "order_cancelled", cancelledAt)).getId();

        SummaryDto summary = summaries.generateOrderSummary(orderId);

        assertThat(summary.cancelledAt()).isEqualTo(cancelledAt.atOffset(java.time.ZoneOffset.UTC));
        assertThat(summary.status()).isEqualTo("CANCELLED");
    }
}
