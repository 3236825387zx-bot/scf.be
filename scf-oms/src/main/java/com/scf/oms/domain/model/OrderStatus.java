package com.scf.oms.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    CREATED(10, "created"),
    ROUTED(20, "routed"),
    LOCKED(30, "locked"),
    DISPATCHED(40, "dispatched"),
    SHIPPED(50, "shipped"),
    DELIVERED(60, "delivered"),
    CANCELLED(90, "cancelled");

    private final Integer code;
    private final String desc;

    public static OrderStatus getByCode(Integer code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
