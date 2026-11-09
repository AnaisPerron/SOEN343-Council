package com.council.tenderapp.prc.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.council.tenderapp.ord.dtos.OrderLineDto;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.ord.services.OrderService;
import com.council.tenderapp.prc.dtos.response.ChargeDto;
import com.council.tenderapp.prc.dtos.response.SummaryDto;
import com.council.tenderapp.prc.entities.Bill;
import com.council.tenderapp.prc.entities.LedgerEntry;
import com.council.tenderapp.prc.exceptions.OrderNotFinishedException;
import com.council.tenderapp.prc.repositories.BillRepository;
import com.council.tenderapp.prc.repositories.LedgerEntryRepository;

@Service
public class SummaryService {
    private final LedgerEntryRepository ledgerRepository;
    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public SummaryService(LedgerEntryRepository ledgerRepository, BillRepository billRepository,
            OrderRepository orderRepository, OrderService orderService) {
        this.ledgerRepository = ledgerRepository;
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public SummaryDto generateOrderSummary(Long orderId) {
        LedgerEntry ledger = ledgerRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFinishedException(
                        "Summary requested but order: " + orderId + " is still being processed."));

        Optional<String> status = ledgerRepository.findFinalOrderStatusById(ledger.getId());
        String restaurantName = orderRepository.findRestaurantNameByOrderId(orderId);
        Optional<OffsetDateTime> finishedAt = ledgerRepository.findTimestampById(ledger.getId());
        List<OrderLineDto> orderLines = orderService.getOrderLinesForOrder(orderId);
        List<ChargeDto> charges = getChargesForOrder(orderId);
        Optional<Double> totalCharge = ledgerRepository.findTotalChargeById(ledger.getId());

        return new SummaryDto(orderId, status.orElse(null), restaurantName, finishedAt.orElse(null),
                orderLines, charges,totalCharge.orElse(null));
    }

    public List<ChargeDto> getChargesForOrder(Long orderId) 
    {
        Bill bill = billRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No bill found for order ID: " + orderId)); 
        
        return List.of(
            new ChargeDto("Service Fee", bill.getServiceFee()),
            new ChargeDto("Delivery Fee", bill.getDeliveryFee()),
            new ChargeDto("Small Order Fee", bill.getSmallOrderFee())
        );
    }
}
