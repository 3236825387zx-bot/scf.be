package com.scf.oms.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class StockUnlockReq {
    private String orderId;
    private String reason;
    private String warehouseId;
    private List<UnlockItem> skuList;

    @Data
    public static class UnlockItem {
        private String skuCode;
        private Integer quantity;
    }
}
