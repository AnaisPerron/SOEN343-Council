package com.council.tenderapp.ord.dtos;

public interface RestaurantBrowseProjection {
    Long getId();
    String getName();
    String getStatus();
    Double getLatitude();
    Double getLongitude();
    String getPhotoUrl();
}
