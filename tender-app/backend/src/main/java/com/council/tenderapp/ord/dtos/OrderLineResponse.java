package com.council.tenderapp.ord.dtos;

import java.math.BigDecimal;

public class OrderLineResponse {

    private final String itemName;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderLineResponse(String itemName, int quantity, BigDecimal unitPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
