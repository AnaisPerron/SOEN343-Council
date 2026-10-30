package com.council.tenderapp.ord.commands;

import com.council.tenderapp.ord.dto.response.OrderResponse;
import com.council.tenderapp.ord.services.OrderService;

public interface OrderCommand {
    OrderResponse execute(OrderService receiver);
}