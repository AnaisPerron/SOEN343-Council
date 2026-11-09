package com.council.tenderapp.ord.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.ord.entities.OrderLine;
import java.util.List;


public interface OrderLineRepository extends JpaRepository<OrderLine, Long> 
{
    List<OrderLine> findByOrderId(Long orderId);
}
