package com.scf.oms.interfaces.dto;

import lombok.Data;

/**
 * 订单取消请求参数
 */
@Data
public class OrderCancelReq {
    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 取消原因
     */
    private String reason;
}
