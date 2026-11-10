package com.council.tenderapp.prc.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.ord.dtos.OrderLineDto;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderEvent;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.OrderNotFoundException;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.repositories.OrderEventRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.prc.dtos.response.ChargeDto;
import com.council.tenderapp.prc.dtos.response.SummaryDto;
import com.council.tenderapp.prc.entities.Bill;
import com.council.tenderapp.prc.entities.LedgerEntry;
import com.council.tenderapp.prc.repositories.BillRepository;
import com.council.tenderapp.prc.repositories.LedgerEntryRepository;

@Service
public class SummaryService {
    private final LedgerEntryRepository ledgerRepository;
    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderService orderService;

    public SummaryService(LedgerEntryRepository ledgerRepository, BillRepository billRepository,
            OrderRepository orderRepository, OrderEventRepository orderEventRepository, OrderService orderService) {
        this.ledgerRepository = ledgerRepository;
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public SummaryDto generateOrderSummary(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order " + orderId + " not found."));
        Optional<LedgerEntry> ledger = ledgerRepository.findByOrderId(orderId);
        String restaurantName = orderRepository.findRestaurantNameByOrderId(orderId);
        List<OrderLineDto> orderLines = orderService.getOrderLinesForOrder(orderId);
        List<ChargeDto> charges = getChargesForOrder(orderId);
        Double totalCharge = ledger.map(LedgerEntry::getTotalCharge)
                .orElseGet(() -> order.getTotalPrice().doubleValue());
        OffsetDateTime cancelledAt = order.getStatus() == OrderStatus.CANCELLED
                ? orderEventRepository.findFirstByOrderIdAndEventTypeOrderByCreatedAtDescIdDesc(orderId, "order_cancelled")
                        .map(OrderEvent::getCreatedAt).map(instant -> instant.atOffset(ZoneOffset.UTC)).orElse(null)
                : null;

        return new SummaryDto(orderId, order.getStatus().name(), restaurantName,
                toOffsetDateTime(order.getDateOrdered()), toOffsetDateTime(order.getAcceptedAt()),
                toOffsetDateTime(order.getPickupDate()), toOffsetDateTime(order.getDateDelivered()),
                cancelledAt, ledger.map(LedgerEntry::getTimestamp).orElse(null), orderLines, charges, totalCharge);
    }

    private static OffsetDateTime toOffsetDateTime(java.util.Date date) {
        return date == null ? null : date.toInstant().atOffset(ZoneOffset.UTC);
    }

    public List<ChargeDto> getChargesForOrder(Long orderId) 
    {
        Optional<Bill> bill = billRepository.findByOrderId(orderId);
        if (bill.isEmpty()) return List.of();
        
        return List.of(
            new ChargeDto("Service Fee", bill.get().getServiceFee()),
            new ChargeDto("Delivery Fee", bill.get().getDeliveryFee()),
            new ChargeDto("Small Order Fee", bill.get().getSmallOrderFee())
        );
    }
}
