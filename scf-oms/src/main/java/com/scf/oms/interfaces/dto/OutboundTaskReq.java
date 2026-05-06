package com.scf.oms.interfaces.dto;

import lombok.Data;
import java.util.List;

/**
 * 出库任务请求参数
 */
@Data
public class OutboundTaskReq {
    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 收件人信息
     */
    private ReceiverInfo receiverInfo;

    /**
     * 商品列表
     */
    private List<OutboundItem> items;

    /**
     * 收件人信息
     */
    @Data
    public static class ReceiverInfo {
        /**
         * 姓名
         */
        private String name;

        /**
         * 电话
         */
        private String phone;

        /**
         * 地址
         */
        private String address;
    }

    /**
     * 出库商品项
     */
    @Data
    public static class OutboundItem {
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
