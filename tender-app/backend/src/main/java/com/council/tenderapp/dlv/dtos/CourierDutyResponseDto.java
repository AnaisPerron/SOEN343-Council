package com.council.tenderapp.dlv.dtos;

import com.council.tenderapp.reg.entities.CourierProfile;

public record CourierDutyResponseDto(
    Long courierId,
    String status,
    Double latitude,
    Double longitude,
    AssignedOrderDto assignedOrder
) {
    public CourierDutyResponseDto(CourierProfile courier, AssignedOrderDto assignedOrder) {
        this(
            courier.getUserId(),
            courier.getStatus().name().toLowerCase(),
            courier.getLatitude(),
            courier.getLongitude(),
            assignedOrder
        );
    }
}
