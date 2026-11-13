package com.council.tenderapp.prc.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerHistoryDto(Long orderId, String status, Instant placedAt,
        Instant pickedUpAt, Instant deliveredAt, BigDecimal totalCharge) {}
