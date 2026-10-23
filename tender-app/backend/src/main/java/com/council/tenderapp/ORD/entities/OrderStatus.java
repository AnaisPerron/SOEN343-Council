package com.council.tenderapp.ORD.entities;

public enum OrderStatus {
    PLACED {
        @Override
        public OrderStatus transitionTo(OrderStatus nextStatus) {
            return switch (nextStatus) {
                case ACCEPTED, REJECTED, CANCELLED -> nextStatus;
                default -> throw invalidTransition(nextStatus);
            };
        }
    },
    ACCEPTED {
        @Override
        public OrderStatus transitionTo(OrderStatus nextStatus) {
            if (nextStatus == PREPARING) {
                return nextStatus;
            }
            throw invalidTransition(nextStatus);
        }
    },
    REJECTED,
    CANCELLED,
    PREPARING {
        @Override
        public OrderStatus transitionTo(OrderStatus nextStatus) {
            if (nextStatus == READY) {
                return nextStatus;
            }
            throw invalidTransition(nextStatus);
        }
    },
    READY {
        @Override
        public OrderStatus transitionTo(OrderStatus nextStatus) {
            if (nextStatus == PICKED_UP || nextStatus == REFUNDED) {
                return nextStatus;
            }
            throw invalidTransition(nextStatus);
        }
    },
    PICKED_UP {
        @Override
        public OrderStatus transitionTo(OrderStatus nextStatus) {
            if (nextStatus == DELIVERED || nextStatus == REFUNDED) {
                return nextStatus;
            }
            throw invalidTransition(nextStatus);
        }
    },
    DELIVERED {
        @Override
        public OrderStatus transitionTo(OrderStatus nextStatus) {
            if (nextStatus == REFUNDED) {
                return nextStatus;
            }
            throw invalidTransition(nextStatus);
        }
    },
    REFUNDED;

    public OrderStatus transitionTo(OrderStatus nextStatus) {
        throw invalidTransition(nextStatus);
    }

    private static IllegalStateException invalidTransition(OrderStatus nextStatus) {
        return new IllegalStateException("Invalid order status transition to " + nextStatus);
    }
}