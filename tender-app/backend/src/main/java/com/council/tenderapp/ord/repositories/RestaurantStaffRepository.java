package com.council.tenderapp.ord.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.council.tenderapp.ord.entities.RestaurantStaff;

public interface RestaurantStaffRepository extends JpaRepository<RestaurantStaff, Long> {
}
