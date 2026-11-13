package com.council.tenderapp.dlv.dtos;


public record PickupConfirmationDto(Long orderId, String orderStatus, String courierStatus) {
}