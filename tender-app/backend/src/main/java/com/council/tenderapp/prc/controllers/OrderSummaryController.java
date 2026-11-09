package com.council.tenderapp.prc.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.services.CustomerOrderHistoryService;
import com.council.tenderapp.prc.dtos.response.SummaryDto;
import com.council.tenderapp.prc.exceptions.UnauthorizedOrderAccessException;
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
    private final CustomerOrderHistoryService orderService;

    public OrderSummaryController(SummaryService summaryService, CustomerOrderHistoryService orderService) {
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
    public SummaryDto getCourierOrderSummary(@PathVariable Long id) 
    {

        SummaryDto summaryDto = summaryService.generateOrderSummary(id);
        return summaryDto;
    }

    @GetMapping("restaurant/{id}")
    @PreAuthorize("hasRole('RESTAURANT')")
    public SummaryDto getRestaurantOrderSummary(@PathVariable Long id) 
    {

        SummaryDto summaryDto = summaryService.generateOrderSummary(id);
        return summaryDto;
    }

    

}
