package com.scf.oms.interfaces.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 路由预估请求参数
 */
@Data
public class RouteReq {
    /** 发货地址（简化为字符串） */
    private String fromAddress;

    /** 收货地址（简化为字符串） */
    private String toAddress;

    /** 重量 (kg) */
    private BigDecimal weight;

    /** 体积 (m3) */
    private BigDecimal volume;
}
