package com.scf.oms.interfaces.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建请求参数
 */
@Data
public class OrderCreateReq {
    /**
     * 外部订单号 (幂等键)
     */
    private String externalOrderId;

    /**
     * 收件人姓名
     */
    private String receiverName;

    /**
     * 收件人电话
     */
    private String receiverPhone;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区/县
     */
    private String district;

    /**
     * 详细地址
     */
    private String detailAddress;

    /**
     * 商品列表
     */
    private List<SkuItem> skuList;

    /**
     * 商品项
     */
    @Data
    public static class SkuItem {
        /**
         * SKU编码
         */
        private String skuCode;

        /**
         * SKU 名称
         */
        private String skuName;

        /**
         * 数量
         */
        private Integer quantity;

        /**
         * 单价
         */
        private BigDecimal price;
    }
}
