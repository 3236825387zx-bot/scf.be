package com.scf.oms.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FulfillmentOrderDetail {
    private final String skuId;
    private final String skuName;
    private final Integer quantity;
    private final BigDecimal price;

    public FulfillmentOrderDetail(String skuId, String skuName, Integer quantity, BigDecimal price) {
        if (skuId == null || skuId.trim().isEmpty()) {
            throw new IllegalArgumentException("skuId must not be blank");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        this.skuId = skuId;
        this.skuName = skuName;
        this.quantity = quantity;
        this.price = price;
    }
}
