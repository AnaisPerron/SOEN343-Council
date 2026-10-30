package com.council.tenderapp.operator.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.operator.dto.RestaurantStatusRequest;
import com.council.tenderapp.operator.services.OperatorRestaurantService;

@RestController
@RequestMapping("/api/operator/restaurants")
public class OperatorRestaurantController {
    private final OperatorRestaurantService service;

    public OperatorRestaurantController(OperatorRestaurantService service) { this.service = service; }

    @PutMapping("/{restaurantId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable @Positive long restaurantId,
            @Valid @RequestBody RestaurantStatusRequest request) {
        service.updateStatus(restaurantId, request.status());
    }
}
