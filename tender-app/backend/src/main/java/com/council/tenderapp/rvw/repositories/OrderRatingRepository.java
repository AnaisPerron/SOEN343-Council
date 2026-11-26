package com.council.tenderapp.rvw.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.council.tenderapp.rvw.entities.OrderRating;

public interface OrderRatingRepository extends JpaRepository<OrderRating, Long> {

    boolean existsByOrderId(Long orderId);

    /** Star totals per restaurant; only ratings inside the 1-5 range count. */
    interface RestaurantStarTotals {
        Long getRestaurantId();
        Long getStarSum();
        Long getRatingCount();
    }

    @Query("""
            SELECT r.restaurantId AS restaurantId, SUM(r.restaurantStars) AS starSum, COUNT(r) AS ratingCount
            FROM OrderRating r
            WHERE r.restaurantId IN :restaurantIds
              AND r.restaurantStars BETWEEN 1 AND 5
            GROUP BY r.restaurantId
            """)
    List<RestaurantStarTotals> restaurantStarTotals(@Param("restaurantIds") Collection<Long> restaurantIds);
}
