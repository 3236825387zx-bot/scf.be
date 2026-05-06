package com.scf.oms.interfaces.dto;

import lombok.Data;

/**
 * SKU库存信息
 */
@Data
public class SkuStockDTO {
    /**
     * SKU编码
     */
    private String skuCode;

    /**
     * 可用数量
     */
    private Integer availableQuantity;

    /**
     * 仓库ID
     */
    private String warehouseId;
}
