package com.council.tenderapp.operator.services;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.tenderapp.operator.dto.RestaurantStatus;
import com.council.tenderapp.ord.exceptions.RestaurantNotFoundException;
import com.council.tenderapp.ord.repositories.RestaurantRepository;

@Service
@PreAuthorize("hasRole('OPERATOR')")
public class OperatorRestaurantService {
    private final RestaurantRepository restaurants;

    public OperatorRestaurantService(RestaurantRepository restaurants) {
        this.restaurants = restaurants;
    }

    @Transactional
    public void updateStatus(long restaurantId, RestaurantStatus status) {
        if (restaurants.updateStatusById(restaurantId, status.value()) == 0) {
            throw new RestaurantNotFoundException("Restaurant not found with id: " + restaurantId);
        }
    }
}
