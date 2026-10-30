package com.council.tenderapp.operator.dto;


public record CourierSummary(
        long id,
        String name,
        String email,
        String phone,
        String status,
        Double latitude,
        Double longitude) {}
