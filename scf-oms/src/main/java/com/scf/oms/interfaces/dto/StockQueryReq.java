package com.scf.oms.interfaces.dto;

import lombok.Data;
import java.util.List;

/**
 * 库存查询请求参数
 */
@Data
public class StockQueryReq {
    /**
     * SKU列表
     */
    private List<String> skuList;

    /**
     * 仓库ID (可选，不传则查询所有)
     */
    private String warehouseId;
}
