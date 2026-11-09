package com.council.tenderapp.reg.entities;

public enum CourierStatus {
    OFFLINE,
    IDLE,
    ASSIGNED,
    CARRYING;

    /** offline → idle; already idle stays idle. */
    public CourierStatus goOnDuty() {
        return switch (this) {
            case OFFLINE, IDLE -> IDLE;
            default -> throw new IllegalStateException("Cannot go on duty while " + label());
        };
    }

    /** idle → offline; already offline stays offline. Refused mid-delivery (R-DLV-03). */
    public CourierStatus goOffDuty() {
        return switch (this) {
            case IDLE, OFFLINE -> OFFLINE;
            default -> throw new IllegalStateException("Cannot go off duty while " + label() + " to an order");
        };
    }

    /** assigned → carrying once the courier has the order; already carrying stays carrying. */
    public CourierStatus pickUp() {
        return switch (this) {
            case ASSIGNED, CARRYING -> CARRYING;
            default -> throw new IllegalStateException("No order to pick up while " + label());
        };
    }

    /** assigned/carrying → idle once the courier's order is finished. */
    public CourierStatus finishDelivery() {
        return switch (this) {
            case ASSIGNED, CARRYING -> IDLE;
            default -> throw new IllegalStateException("No delivery to finish while " + label());
        };
    }

    private String label() {
        return name().toLowerCase();
    }
}
