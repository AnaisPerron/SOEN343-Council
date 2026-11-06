package com.council.tenderapp.ord.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Image uploaded by restaurant staff; menu items reference it as /menu-photos/{id}. */
@Entity
@Table(name = "menu_photos")
public class MenuPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    protected MenuPhoto() {}

    public MenuPhoto(Long restaurantId, String contentType, byte[] data) {
        this.restaurantId = restaurantId;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getId() { return id; }
    public Long getRestaurantId() { return restaurantId; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}
