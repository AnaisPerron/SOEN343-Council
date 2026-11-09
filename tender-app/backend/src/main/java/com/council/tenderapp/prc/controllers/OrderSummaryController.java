package com.council.tenderapp.prc.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.dlv.services.CourierDutyService;
import com.council.tenderapp.prc.dtos.response.SummaryDto;
import com.council.tenderapp.prc.exceptions.UnauthorizedOrderAccessException;
import com.council.tenderapp.prc.service.OrderBelongingService;
import com.council.tenderapp.prc.service.SummaryService;
import com.council.tenderapp.reg.security.Session;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/order-summary")
public class OrderSummaryController 
{
    private final SummaryService summaryService;
    private final OrderBelongingService orderService;

    public OrderSummaryController(SummaryService summaryService, OrderBelongingService orderService, CourierDutyService courierDutyService) {
        this.summaryService = summaryService;
        this.orderService = orderService;
    }

    @GetMapping("customer/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public SummaryDto getCustomerOrderSummary(@PathVariable Long id, Authentication auth) 
    {
        Session session = (Session) auth.getDetails();
        Long customerId = session.accountId();
        if(!orderService.isCustomerOrder(id, customerId)) {
            throw new UnauthorizedOrderAccessException("Order " + id + " does not belong to customer " + customerId + " or does not exist.");
        }
        SummaryDto summaryDto = summaryService.generateOrderSummary(id);
        return summaryDto;
    }

    @GetMapping("operator/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public SummaryDto getOperatorOrderSummary(@PathVariable Long id) 
    {
        SummaryDto summaryDto = summaryService.generateOrderSummary(id);
        return summaryDto;
    }

    @GetMapping("courier/{id}")
    @PreAuthorize("hasRole('COURIER')")
    public SummaryDto getCourierOrderSummary(@PathVariable Long id, Authentication auth) 
    {
        Session session = (Session) auth.getDetails();
        Long courierId = session.accountId();
        if(!orderService.isCourierOrder(id, courierId)) {
            throw new UnauthorizedOrderAccessException("Order " + id + " was not delivered by courier " + courierId + " or does not exist.");
        }
        SummaryDto summaryDto = summaryService.generateOrderSummary(id);
        return summaryDto;
    }

    @GetMapping("restaurant/{id}")
    @PreAuthorize("hasRole('RESTAURANT')")
    public SummaryDto getRestaurantOrderSummary(@PathVariable Long id, Authentication auth) 
    {
        Session session = (Session) auth.getDetails();
        Long restaurantId = session.accountId();
        if(!orderService.isRestaurantOrder(id, restaurantId)) {
            throw new UnauthorizedOrderAccessException("Order " + id + " does not belong to restaurant " + restaurantId + " or does not exist.");
        }
        SummaryDto summaryDto = summaryService.generateOrderSummary(id);
        return summaryDto;
    }
}
