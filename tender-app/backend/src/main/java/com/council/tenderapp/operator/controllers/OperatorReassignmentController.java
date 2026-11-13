package com.council.tenderapp.operator.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.operator.dto.ReassignOrderRequest;
import com.council.tenderapp.operator.services.OperatorReassignmentService;

@RestController
@RequestMapping("/api/operator/orders")
public class OperatorReassignmentController {
    private final OperatorReassignmentService service;

    public OperatorReassignmentController(OperatorReassignmentService service) { this.service = service; }

    @PostMapping("/{orderId}/reassign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reassign(@PathVariable @Positive long orderId, @Valid @RequestBody ReassignOrderRequest request) {
        service.reassign(orderId, request.expectedCourierId());
    }
}
