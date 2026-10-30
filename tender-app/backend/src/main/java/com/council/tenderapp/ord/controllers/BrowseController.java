package com.council.tenderapp.ord.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.dtos.MenuResponseDTO;
import com.council.tenderapp.ord.dtos.RestaurantResponseDTO;
import com.council.tenderapp.ord.services.BrowseService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController 
@PreAuthorize("hasRole('CUSTOMER')")
public class BrowseController 
{
    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    @GetMapping("/api/restaurant/open")
    public List<RestaurantResponseDTO> getOpenRestaurants() {
        return browseService.getOpenRestaurants();
    }

    @GetMapping("/api/restaurant/{restaurantId}/menu")
    public MenuResponseDTO getRestaurantMenu(@PathVariable Long restaurantId) {
        browseService.checkRestaurantOpen(restaurantId);
        return browseService.getRestaurantMenu(restaurantId);
    }
}
