package com.council.tenderapp.ord.repositories;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByDateOrderedDescIdDesc(Long customerId);

    // All statuses are needed by the restaurant dashboard, ordered as a FIFO queue.
    List<Order> findByRestaurantIdOrderByDateOrderedAscIdAsc(Long restaurantId);

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

    Optional<Order> findFirstByCourierIdAndStatusInOrderByDateOrderedAscIdAsc(Long courierId, Collection<OrderStatus> statuses);

    // Dispatch queue: ready orders still waiting for a courier, oldest first
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Order> findByStatusAndCourierIdIsNullOrderByDateOrderedAscIdAsc(OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findWithLockById(@Param("orderId") Long orderId);
}
