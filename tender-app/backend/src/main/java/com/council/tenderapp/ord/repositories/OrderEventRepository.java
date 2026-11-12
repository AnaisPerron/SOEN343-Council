package com.council.tenderapp.ord.repositories;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.council.tenderapp.ord.entities.OrderEvent;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    List<OrderEvent> findByOrderIdInAndEventType(Collection<Long> orderIds, String eventType);

    boolean existsByOrderIdAndEventType(Long orderId, String eventType);
}
