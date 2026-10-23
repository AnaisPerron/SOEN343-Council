package com.council.tenderapp.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// These mappings let Hibernate add display/edit fields to an existing demo schema.
final class MenuSchema {
    private MenuSchema() {}

    @Entity @Table(name = "restaurants")
    static class RestaurantRow {
        protected RestaurantRow() {}
        @Id Long id;
        String name;
        String status;
        Double latitude;
        Double longitude;
        String address;
        @Column(name = "delivery_fee") java.math.BigDecimal deliveryFee;
        @Column(name = "cover_url") String coverUrl;
    }

    @Entity @Table(name = "menu_items")
    static class ItemRow {
        protected ItemRow() {}
        @Id Long id;
        @Column(name = "menu_id") Long menuId;
        String name;
        java.math.BigDecimal price;
        String status;
        @Column(name = "photo_url") String photoUrl;
        @Column(columnDefinition = "TEXT") String description;
        String category;
    }
}
