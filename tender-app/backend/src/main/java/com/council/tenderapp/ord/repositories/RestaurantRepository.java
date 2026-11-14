package com.council.tenderapp.ord.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.council.tenderapp.ord.dtos.RestaurantBrowseProjection;
import com.council.tenderapp.ord.entities.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    
    @Query(value = """
            SELECT r.id, r.name, r.status, r.latitude, r.longitude,
            COALESCE(NULLIF(TRIM(r.cover_url), ''), (SELECT mi.photo_url
            FROM menu_items mi
            JOIN menus m ON mi.menu_id = m.id
            WHERE m.restaurant_id= r.id AND NULLIF(TRIM(mi.photo_url), '') IS NOT NULL
            ORDER BY mi.id
            LIMIT 1)) AS photo_url
            FROM restaurants r
            WHERE r.status = :status
            """, nativeQuery = true)
    List<RestaurantBrowseProjection> findRestaurantBrowseByStatus(String status);

    Optional<Restaurant> findByName(String name);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Restaurant r SET r.status = :status WHERE r.id = :restaurantId")
    int updateStatusById(@Param("restaurantId") Long restaurantId, @Param("status") String status);
}
