package com.council.tenderapp.ORD.persistence;

public class OrderItemResponse {

    private final String itemName;
    private final int quantity;

    public OrderItemResponse(String itemName, int quantity) {
        this.itemName = itemName;
        this.quantity = quantity;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }
}
