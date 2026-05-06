package com.scf.oms.interfaces.dto;

import lombok.Data;
import java.util.List;

/**
 * 库存锁定请求参数
 */
@Data
public class StockLockReq {
    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 锁定商品列表
     */
    private List<LockItem> skuList;

    /**
     * 锁定商品项
     */
    @Data
    public static class LockItem {
        /**
         * SKU编码
         */
        private String skuCode;

        /**
         * 数量
         */
        private Integer quantity;
    }
}
