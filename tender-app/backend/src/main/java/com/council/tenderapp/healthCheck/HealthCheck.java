package com.council.tenderapp.healthCheck;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "health_check")
public class HealthCheck {

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    protected HealthCheck() {
    }
    public HealthCheck(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    @Id 
    private Long id;

    private String status;
}
