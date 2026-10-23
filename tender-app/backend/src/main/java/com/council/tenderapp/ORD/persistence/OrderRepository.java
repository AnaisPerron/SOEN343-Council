package com.council.tenderapp.ORD.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
        Long restaurantId,
        Collection<OrderStatus> statuses
    );
}
