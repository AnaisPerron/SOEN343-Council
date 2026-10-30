package com.council.tenderapp.operator.dto;


public record DashboardSummary(
        long restaurantCount,
        long orderCount,
        long customerCount,
        long courierCount,
        long availableCourierCount) {}
