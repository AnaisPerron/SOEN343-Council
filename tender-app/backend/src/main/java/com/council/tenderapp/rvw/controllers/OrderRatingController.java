package com.council.tenderapp.rvw.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.rvw.dtos.OrderRatingResponseDto;
import com.council.tenderapp.rvw.dtos.SubmitRatingRequestDto;
import com.council.tenderapp.rvw.services.OrderRatingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders/{orderId}/rating")
public class OrderRatingController {
    private final OrderRatingService ratings;

    public OrderRatingController(OrderRatingService ratings) {
        this.ratings = ratings;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderRatingResponseDto rate(@AuthenticationPrincipal Long customerId, @PathVariable Long orderId,
            @Valid @RequestBody SubmitRatingRequestDto request) {
        return ratings.rate(customerId, orderId, request);
    }
}
