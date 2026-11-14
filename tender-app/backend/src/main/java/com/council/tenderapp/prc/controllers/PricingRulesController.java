package com.council.tenderapp.prc.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.council.tenderapp.prc.service.PricingService;

@RestController
@RequestMapping("/api/pricing")
public class PricingRulesController {
    private final PricingService pricing;

    public PricingRulesController(PricingService pricing) {
        this.pricing = pricing;
    }

    @GetMapping("/rules")
    public PricingService.Rules rules() {
        return pricing.rules();
    }
}
