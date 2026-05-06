package com.scf.oms.interfaces.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 路由预估结果
 */
@Data
public class RouteEstimateDTO {
    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 预估运费
     */
    private BigDecimal estimatedCost;

    /**
     * 预估时效 (小时)
     */
    private BigDecimal estimatedTime;
}
