package com.council.tenderapp.ord.commands;

import com.council.tenderapp.ord.dtos.OrderResponse;
import com.council.tenderapp.ord.services.OrderService;

public final class OrderCommands {

    private OrderCommands() {
    }

    public record Accept(Long restaurantId, Long orderId) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.acceptOrder(restaurantId, orderId);
        }
    }

    public record Reject(Long restaurantId, Long orderId, String reason) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.rejectOrder(restaurantId, orderId, reason);
        }
    }

    public record Cancel(Long restaurantId, Long orderId) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.cancelOrder(restaurantId, orderId);
        }
    }

    public record StartPreparation(Long restaurantId, Long orderId) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.startPreparation(restaurantId, orderId);
        }
    }

    public record MarkReady(Long restaurantId, Long orderId) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.markReady(restaurantId, orderId);
        }
    }

    public record MarkPickedUp(Long restaurantId, Long orderId) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.markPickedUp(restaurantId, orderId);
        }
    }

    public record MarkDelivered(Long restaurantId, Long orderId) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.markDelivered(restaurantId, orderId);
        }
    }

    public record Refund(Long restaurantId, Long orderId) implements OrderCommand {
        @Override
        public OrderResponse execute(OrderService receiver) {
            return receiver.refundOrder(restaurantId, orderId);
        }
    }
}