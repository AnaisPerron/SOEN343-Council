package com.council.tenderapp.rvw.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.rvw.entities.OrderRating;

public interface OrderRatingRepository extends JpaRepository<OrderRating, Long> {

    boolean existsByOrderId(Long orderId);
}
