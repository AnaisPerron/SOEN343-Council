package com.council.tenderapp.dlv.dtos;

public record DeliveryConfirmationDto(Long orderId, String orderStatus, String courierStatus) {
}