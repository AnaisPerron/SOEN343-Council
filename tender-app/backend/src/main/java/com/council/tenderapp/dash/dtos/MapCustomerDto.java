package com.council.tenderapp.dash.dtos;

public record MapCustomerDto(
    Long customerId,
    String name,
    double latitude,
    double longitude
) {}
