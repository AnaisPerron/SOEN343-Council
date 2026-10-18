package com.council.tenderapp.healthCheck;

import org.springframework.stereotype.Service;

@Service 
public class HealthService {

    private final HealthRepository repository;
    
    public HealthService(HealthRepository repository) {
        this.repository = repository;
    }
    
    public String checkHealth() {
        return repository.findById(1L)
        .orElseThrow()
        .getStatus();
    }
}
