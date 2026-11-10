package com.council.tenderapp.ord.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.council.tenderapp.ord.entities.OrderEvent;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    Optional<OrderEvent> findFirstByOrderIdAndEventTypeOrderByCreatedAtDescIdDesc(Long orderId, String eventType);
}
