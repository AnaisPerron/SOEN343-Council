package com.council.tenderapp.operator.dto;

import java.math.BigDecimal;

public record CustomerSummary(
        long id,
        String name,
        String email,
        String phone,
        BigDecimal tdCashBalance,
        Double latitude,
        Double longitude) {}
