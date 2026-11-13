package com.council.tenderapp.prc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.prc.dtos.response.SummaryDto;
import com.council.tenderapp.prc.entities.Bill;
import com.council.tenderapp.prc.entities.LedgerEntry;
import com.council.tenderapp.prc.repositories.BillRepository;
import com.council.tenderapp.prc.repositories.LedgerEntryRepository;
import com.council.tenderapp.prc.service.SummaryService;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {
    @Mock private LedgerEntryRepository ledgers;
    @Mock private BillRepository bills;
    @Mock private OrderRepository orders;
    @Mock private OrderEventRepository events;
    @Mock private OrderService orderService;

    @Test
    void activeOrderHasSummaryBeforeLedgerEntryExists() {
        Order order = new Order(7L, Date.from(OffsetDateTime.parse("2026-11-10T12:00:00Z").toInstant()), OrderStatus.ACCEPTED);
        order.setTotalPrice(new BigDecimal("24.50"));
        order.setAcceptedAt(Date.from(OffsetDateTime.parse("2026-11-10T12:02:00Z").toInstant()));
        Bill bill = new Bill(4L, 24L, 2L, 20.0, null, 2.0, 2.5, 0.0);
        when(orders.findById(24L)).thenReturn(Optional.of(order));
        when(orders.findRestaurantNameByOrderId(24L)).thenReturn("Cafe");
        when(ledgers.findByOrderId(24L)).thenReturn(Optional.empty());
        when(bills.findByOrderId(24L)).thenReturn(Optional.of(bill));
        when(orderService.getOrderLinesForOrder(24L)).thenReturn(List.of());

        SummaryDto summary = new SummaryService(ledgers, bills, orders, events, orderService).generateOrderSummary(24L);

        assertThat(summary.status()).isEqualTo("ACCEPTED");
        assertThat(summary.placedAt()).isEqualTo(OffsetDateTime.of(2026, 11, 10, 12, 0, 0, 0, ZoneOffset.UTC));
        assertThat(summary.acceptedAt()).isEqualTo(OffsetDateTime.of(2026, 11, 10, 12, 2, 0, 0, ZoneOffset.UTC));
        assertThat(summary.finishedAt()).isNull();
        assertThat(summary.totalCharge()).isEqualTo(24.5);
        assertThat(summary.charges()).hasSize(3);
    }

    @Test
    void completedOrderIncludesLedgerFinishTime() {
        Order order = new Order(7L, new Date(), OrderStatus.DELIVERED);
        order.setTotalPrice(new BigDecimal("24.50"));
        LedgerEntry ledger = new LedgerEntry();
        ledger.setTimestamp(OffsetDateTime.parse("2026-11-10T12:45:00Z"));
        ledger.setTotalCharge(24.5);
        when(orders.findById(24L)).thenReturn(Optional.of(order));
        when(ledgers.findByOrderId(24L)).thenReturn(Optional.of(ledger));
        when(bills.findByOrderId(24L)).thenReturn(Optional.empty());
        when(orderService.getOrderLinesForOrder(24L)).thenReturn(List.of());

        SummaryDto summary = new SummaryService(ledgers, bills, orders, events, orderService).generateOrderSummary(24L);

        assertThat(summary.finishedAt()).isEqualTo(OffsetDateTime.parse("2026-11-10T12:45:00Z"));
        assertThat(summary.status()).isEqualTo("DELIVERED");
    }

    @Test
    void pickupAndDeliveryTransitionsRecordTheirTimes() {
        Order order = new Order(7L, new Date(), OrderStatus.READY);

        order.transitionTo(OrderStatus.PICKED_UP);
        assertThat(order.getPickupDate()).isNotNull();

        order.transitionTo(OrderStatus.DELIVERED);
        assertThat(order.getDateDelivered()).isNotNull();
    }
}
