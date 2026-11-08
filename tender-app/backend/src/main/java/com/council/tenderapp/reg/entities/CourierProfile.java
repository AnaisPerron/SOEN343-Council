package com.council.tenderapp.reg.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Courier-specific data for a users row with role courier: delivery state and current location. */
@Entity
@Table(name = "courier_profiles")
public class CourierProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Convert(converter = CourierStatusConverter.class)
    @Column(nullable = false, length = 20)
    private CourierStatus status = CourierStatus.OFFLINE;

    private Double latitude;

    private Double longitude;

    protected CourierProfile() {}

    // New couriers start offline with no known location
    public CourierProfile(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public CourierStatus getStatus() { return status; }
    public void setStatus(CourierStatus status) { this.status = status; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
