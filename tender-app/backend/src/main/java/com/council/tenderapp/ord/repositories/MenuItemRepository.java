package com.council.tenderapp.ord.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.council.tenderapp.ord.entities.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    
    @Query(value = """
            SELECT mi.* 
            FROM menu_items mi
            JOIN  menus m 
            ON mi.menu_id = m.id
            WHERE m.restaurant_id = :restaurantId
            """, nativeQuery = true)
    List<MenuItem> findMenuItemsByRestaurantId(@Param ("restaurantId") Long restaurantId);
}
