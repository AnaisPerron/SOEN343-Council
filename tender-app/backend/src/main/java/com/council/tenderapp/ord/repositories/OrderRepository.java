package com.council.tenderapp.ord.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.ord.entities.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
