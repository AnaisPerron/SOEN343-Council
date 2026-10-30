package com.council.tenderapp.operator.controllers;

import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.services.OrderCancellationService;

@RestController
@RequestMapping("/api/operator/orders")
public class OperatorOrderController {
    private final OrderCancellationService cancellations;

    public OperatorOrderController(OrderCancellationService cancellations) {
        this.cancellations = cancellations;
    }

    @PostMapping("/{orderId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable @Positive Long orderId) {
        cancellations.cancelForOperator(orderId);
    }
}
