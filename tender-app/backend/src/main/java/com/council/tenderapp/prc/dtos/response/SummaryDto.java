package com.council.tenderapp.prc.dtos.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.council.tenderapp.ord.dtos.OrderLineDto;

public record SummaryDto (
    Long orderId,
    String status,
    String restaurantName,
    OffsetDateTime finishedAt,
    List<OrderLineDto> orderItems,
    List<ChargeDto> charges,
    Double totalCharge
) 
{}