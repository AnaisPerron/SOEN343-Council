package com.council.tenderapp.healthCheck;

import org.springframework.stereotype.Service;

import com.council.tenderapp.technicalservices.logging.AppLogService;

@Service 
public class HealthService {

    private final HealthRepository repository;
    private final AppLogService appLog;
    
    public HealthService(HealthRepository repository, AppLogService appLog) {
        this.repository = repository;
        this.appLog = appLog;
    }
    
    public String checkHealth() {
        appLog.info("Checking database health");

        try {
            String status = repository.findById(1L)
                .orElseThrow()
                .getStatus();

            appLog.info("Database health check succeeded");
            return status;
        } catch (RuntimeException error) {
            appLog.error("Database health check failed", error);
            throw error;
        }
    }
}
