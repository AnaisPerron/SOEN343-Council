package com.council.tenderapp.ORD.repositories;

import com.council.tenderapp.ORD.entities.Order;
import com.council.tenderapp.ORD.entities.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByRestaurantIdAndStatusOrderByDateOrderedAscIdAsc(
        Long restaurantId,
        OrderStatus status
    );

    List<Order> findByRestaurantIdAndStatusInOrderByAcceptedAtAscIdAsc(
        Long restaurantId,
        Collection<OrderStatus> statuses
    );

    List<Order> findByRestaurantIdAndStatusIn(
        Long restaurantId,
        Collection<OrderStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);
}