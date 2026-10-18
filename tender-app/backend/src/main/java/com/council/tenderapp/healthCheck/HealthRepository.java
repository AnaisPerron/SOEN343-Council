package com.council.tenderapp.healthCheck;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRepository extends JpaRepository<HealthCheck, Long> {}
