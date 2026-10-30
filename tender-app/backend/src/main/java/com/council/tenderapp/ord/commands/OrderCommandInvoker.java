package com.council.tenderapp.ord.commands;

import org.springframework.stereotype.Component;

import com.council.tenderapp.ord.dto.response.OrderResponse;
import com.council.tenderapp.ord.services.OrderService;

@Component
public class OrderCommandInvoker {

    private final OrderService orderService;

    public OrderCommandInvoker(OrderService orderService) {
        this.orderService = orderService;
    }

    public OrderResponse execute(OrderCommand command) {
        return command.execute(orderService);
    }
}