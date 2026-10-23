package com.council.tenderapp.reg.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class TestController {

    @GetMapping("/api/test")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STAFF')")
    public String getTest() {
        return "Test endpoint is working!";
    }

}
