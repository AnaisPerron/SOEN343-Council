package com.council.tenderapp.ord.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.council.tenderapp.ord.dtos.RestaurantBrowseProjection;
import com.council.tenderapp.ord.entities.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    
    @Query(value = """
            SELECT r.id, r.name, r.status, r.latitude, r.longitude,
            (SELECT mi.photo_url
            FROM menu_items mi
            JOIN menus m ON mi.menu_id = m.id
            WHERE m.restaurant_id= r.id AND mi.photo_url IS NOT NULL
            LIMIT 1) AS photo_url
            FROM restaurants r
            WHERE r.status = :status
            """, nativeQuery = true)
    List<RestaurantBrowseProjection> findRestaurantBrowseByStatus(String status);

    Optional<Restaurant> findByName(String name);
}
