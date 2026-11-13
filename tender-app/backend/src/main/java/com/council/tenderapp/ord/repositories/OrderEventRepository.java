package com.council.tenderapp.ord.repositories;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.council.tenderapp.ord.entities.OrderEvent;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    Optional<OrderEvent> findFirstByOrderIdAndEventTypeOrderByCreatedAtDescIdDesc(Long orderId, String eventType);

    List<OrderEvent> findByOrderIdInAndEventType(Collection<Long> orderIds, String eventType);

    boolean existsByOrderIdAndEventType(Long orderId, String eventType);
}
