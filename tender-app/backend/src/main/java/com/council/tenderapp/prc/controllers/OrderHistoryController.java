package com.council.tenderapp.prc.controllers;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.repositories.OrderRepository;
import com.council.tenderapp.prc.dtos.response.LedgerHistoryDto;
import com.council.tenderapp.reg.security.Session;

@RestController
@RequestMapping("/api/pricing/history")
public class OrderHistoryController {
    private final OrderRepository orders;

    public OrderHistoryController(OrderRepository orders) { this.orders = orders; }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'COURIER', 'OPERATOR')")
    public List<LedgerHistoryDto> history(Authentication authentication) {
        Session session = (Session) authentication.getDetails();
        List<Order> permitted = switch (session.role()) {
            case CUSTOMER -> orders.findByCustomerIdOrderByDateOrderedDescIdDesc(session.accountId());
            case STAFF -> orders.findByRestaurantIdOrderByDateOrderedDescIdDesc(session.restaurantId());
            case COURIER -> orders.findByCourierIdAndStatusInOrderByDateOrderedDescIdDesc(
                    session.accountId(), List.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED));
            case OPERATOR -> orders.findAll();
        };
        return permitted.stream().map(order -> new LedgerHistoryDto(order.getId(), order.getStatus().name(),
                instant(order.getDateOrdered()), instant(order.getPickupDate()),
                instant(order.getDateDelivered()), order.getTotalPrice())).toList();
    }

    private static java.time.Instant instant(java.util.Date date) {
        return date == null ? null : date.toInstant();
    }
}
