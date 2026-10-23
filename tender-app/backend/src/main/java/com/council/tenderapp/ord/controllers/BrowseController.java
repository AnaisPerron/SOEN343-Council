package com.council.tenderapp.ord.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
public class BrowseController 
{
    @GetMapping("/api/restaurants")
    public List<Restaurant> getMethodName(@RequestParam String param) {
        return new String();
    }
    
}
