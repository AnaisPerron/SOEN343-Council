package com.council.tenderapp.ord.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurant_staff")
public class RestaurantStaff {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    protected RestaurantStaff() {}

    public RestaurantStaff(Long userId, Long restaurantId) {
        this.userId = userId;
        this.restaurantId = restaurantId;
    }

    public Long getUserId() { return userId; }
    public Long getRestaurantId() { return restaurantId; }
}
