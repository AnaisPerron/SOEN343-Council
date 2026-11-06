package com.council.tenderapp.reg.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.reg.entities.CourierProfile;

public interface CourierProfileRepository extends JpaRepository<CourierProfile, Long> {
}
