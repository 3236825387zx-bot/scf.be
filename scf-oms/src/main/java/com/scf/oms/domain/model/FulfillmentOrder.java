package com.scf.oms.domain.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
public class FulfillmentOrder {
    private final String orderId;
    private final String externalOrderId;
    private Integer status;
    private final ReceiverInfo receiverInfo;
    private final List<FulfillmentOrderDetail> details;
    private String warehouseId;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public FulfillmentOrder(String orderId, String externalOrderId, ReceiverInfo receiverInfo) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (externalOrderId == null || externalOrderId.trim().isEmpty()) {
            throw new IllegalArgumentException("externalOrderId must not be blank");
        }
        this.orderId = orderId;
        this.externalOrderId = externalOrderId;
        this.receiverInfo = receiverInfo;
        this.status = OrderStatus.CREATED.getCode();
        this.details = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    public void addDetail(String skuId, String skuName, Integer quantity, BigDecimal price) {
        details.add(new FulfillmentOrderDetail(skuId, skuName, quantity, price));
        totalAmount = details.stream()
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        updateTime = LocalDateTime.now();
    }

    public void assignWarehouse(String warehouseId) {
        if (!OrderStatus.CREATED.getCode().equals(status)) {
            throw new IllegalStateException("warehouse can only be assigned from created status");
        }
        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            throw new IllegalArgumentException("warehouseId must not be blank");
        }
        this.warehouseId = warehouseId;
        this.status = OrderStatus.ROUTED.getCode();
        this.updateTime = LocalDateTime.now();
        log.info("Order {} assigned to warehouse {}", orderId, warehouseId);
    }

    public void stockLocked() {
        if (!OrderStatus.ROUTED.getCode().equals(status)) {
            throw new IllegalStateException("stock can only be locked from routed status");
        }
        this.status = OrderStatus.LOCKED.getCode();
        this.updateTime = LocalDateTime.now();
        log.info("Order {} stock locked", orderId);
    }

    public void dispatched() {
        if (!OrderStatus.LOCKED.getCode().equals(status)) {
            throw new IllegalStateException("order can only be dispatched from locked status");
        }
        this.status = OrderStatus.DISPATCHED.getCode();
        this.updateTime = LocalDateTime.now();
        log.info("Order {} dispatched to WMS", orderId);
    }

    public void cancel() {
        if (status >= OrderStatus.SHIPPED.getCode()) {
            throw new IllegalStateException("shipped orders cannot be cancelled");
        }
        if (OrderStatus.CANCELLED.getCode().equals(status)) {
            log.warn("Order {} is already cancelled", orderId);
            return;
        }
        this.status = OrderStatus.CANCELLED.getCode();
        this.updateTime = LocalDateTime.now();
        log.info("Order {} cancelled", orderId);
    }

    public static FulfillmentOrder rehydrate(
            String orderId,
            String externalOrderId,
            ReceiverInfo receiverInfo,
            Integer status,
            String warehouseId,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            List<FulfillmentOrderDetail> details
    ) {
        FulfillmentOrder order = new FulfillmentOrder(orderId, externalOrderId, receiverInfo);
        order.status = status;
        order.warehouseId = warehouseId;
        order.details.clear();
        order.details.addAll(details);
        order.totalAmount = details.stream()
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.updateTime = updateTime;
        order.createTime = createTime;
        return order;
    }
}
