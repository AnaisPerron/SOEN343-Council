package com.council.tenderapp.ord.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.ord.entities.OrderEvent;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
}
