package com.council.tenderapp.ord.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.ord.dtos.RestaurantResponseDTO;
import com.council.tenderapp.ord.services.BrowseService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

@RestController 
public class BrowseController 
{
    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    @GetMapping("/api/restaurants")
    public List<RestaurantResponseDTO> getOpenRestaurants() {
        return browseService.getOpenRestaurants();
    }
    
}
