package com.council.tenderapp.healthCheck;

import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/health")
public class HealthController {

    public final HealthService service;

    public HealthController(HealthService service) {
        this.service = service;
    }

    @GetMapping
    public String checkHealth() {
        return service.checkHealth();
    }
}
