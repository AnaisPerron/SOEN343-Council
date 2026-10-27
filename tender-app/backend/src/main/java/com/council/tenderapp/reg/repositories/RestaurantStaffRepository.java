package com.council.tenderapp.reg.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.reg.entities.RestaurantStaff;

public interface RestaurantStaffRepository extends JpaRepository<RestaurantStaff, Long> {
    Optional<RestaurantStaff> findByUserId(Long userId);
    
}
