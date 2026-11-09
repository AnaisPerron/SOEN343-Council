package com.council.tenderapp.prc.repositories;

import java.util.Optional;

import com.council.tenderapp.prc.entities.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
    
    Optional<Bill> findByOrderId(Long orderId); 
}
