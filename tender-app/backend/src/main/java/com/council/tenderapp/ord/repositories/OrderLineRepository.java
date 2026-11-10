package com.council.tenderapp.ord.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.council.tenderapp.ord.entities.OrderLine;
import java.util.List;


public interface OrderLineRepository extends JpaRepository<OrderLine, Long> 
{
    @Query("SELECT line FROM OrderLine line JOIN FETCH line.menuItem WHERE line.order.id = :orderId")
    List<OrderLine> findByOrderId(@Param("orderId") Long orderId);
}
