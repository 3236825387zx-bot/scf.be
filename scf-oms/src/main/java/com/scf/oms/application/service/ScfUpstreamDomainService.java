package com.scf.oms.application.service;

import com.scf.oms.interfaces.dto.OrderCreateReq;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

class ScfUpstreamDomainService {

    private final ScfFacadeSupport support;
    private final FulfillmentOrderService fulfillmentOrderService;

    ScfUpstreamDomainService(ScfFacadeSupport support, FulfillmentOrderService fulfillmentOrderService) {
        this.support = support;
        this.fulfillmentOrderService = fulfillmentOrderService;
    }

    Map<String, Object> upstreamOrders(Map<String, String> query) {
        List<Map<String, Object>> rows = support.jdbc().query(
                "select * from upstream_order order by create_time desc",
                (rs, rowNum) -> support.map(
                        "id", rs.getLong("id"),
                        "upstreamOrderNo", rs.getString("upstream_order_no"),
                        "externalNo", rs.getString("external_no"),
                        "channel", rs.getString("channel_code"),
                        "receiverName", rs.getString("receiver_name"),
                        "receiverPhone", rs.getString("receiver_phone"),
                        "province", rs.getString("province"),
                        "city", rs.getString("city"),
                        "district", rs.getString("district"),
                        "detailAddress", rs.getString("detail_address"),
                        "tempLayer", rs.getString("temp_layer"),
                        "requestedDelivery", rs.getString("requested_delivery"),
                        "totalAmount", support.money(rs.getBigDecimal("total_amount")),
                        "status", rs.getString("status"),
                        "statusText", rs.getString("status_text"),
                        "dispatchTime", support.format(rs.getTimestamp("dispatch_time")),
                        "fulfillmentOrderNo", rs.getString("fulfillment_order_no"),
                        "targetWarehouseName", rs.getString("target_warehouse_name"),
                        "createTime", support.format(rs.getTimestamp("create_time"))
                )
        ).stream()
                .filter(row -> support.contains(row, "upstreamOrderNo", query.get("upstreamOrderNo")))
                .filter(row -> support.contains(row, "externalNo", query.get("externalNo")))
                .filter(row -> support.contains(row, "receiverName", query.get("receiverName")))
                .filter(row -> support.equalsValue(row, "channel", query.get("channel")))
                .filter(row -> support.equalsValue(row, "status", query.get("status")))
                .toList();
        Map<String, Object> result = support.page(rows, query);
        result.put("orders", result.get("list"));
        result.put("statusOptions", support.prependAll(support.simpleOptions("pending", "待下发", "dispatched", "已下发")));
        result.put("channelOptions", support.prependAll(support.jdbc().query(
                "select distinct channel_code from upstream_order where channel_code is not null and channel_code <> '' order by channel_code",
                (rs, rowNum) -> support.option(support.channelLabel(rs.getString(1)), rs.getString(1))
        )));
        result.put("defaults", support.map(
                "upstreamOrderNo", "",
                "externalNo", "",
                "receiverName", "",
                "channel", "",
                "status", ""
        ));
        return result;
    }

    Map<String, Object> dispatchUpstreamOrder(Long id) {
        Map<String, Object> upstream = support.requiredOne("select * from upstream_order where id = ?", "upstream order not found: " + id, id);
        String fulfillmentOrderNo = support.s(upstream.get("fulfillment_order_no"));
        if (support.blank(fulfillmentOrderNo)) {
            OrderCreateReq req = new OrderCreateReq();
            req.setExternalOrderId(support.s(upstream.get("external_no")));
            req.setReceiverName(support.s(upstream.get("receiver_name")));
            req.setReceiverPhone(support.s(upstream.get("receiver_phone")));
            req.setProvince(support.s(upstream.get("province")));
            req.setCity(support.s(upstream.get("city")));
            req.setDistrict(support.s(upstream.get("district")));
            req.setDetailAddress(support.s(upstream.get("detail_address")));
            req.setSkuList(support.jdbc().query(
                    "select * from upstream_order_item where upstream_order_no = ? order by id",
                    (rs, rowNum) -> {
                        OrderCreateReq.SkuItem item = new OrderCreateReq.SkuItem();
                        item.setSkuCode(rs.getString("sku_code"));
                        item.setSkuName(rs.getString("sku_name"));
                        item.setQuantity(rs.getInt("quantity"));
                        item.setPrice(rs.getBigDecimal("unit_price"));
                        return item;
                    },
                    support.s(upstream.get("upstream_order_no"))
            ));
            fulfillmentOrderNo = fulfillmentOrderService.createOrder(req);
            Map<String, Object> omsOrder = support.requiredOne("select warehouse_name from oms_order where order_no = ?", "oms order not found: " + fulfillmentOrderNo, fulfillmentOrderNo);
            Timestamp now = support.now();
            support.jdbc().update(
                    "update upstream_order set status = 'dispatched', status_text = '已下发', dispatch_time = ?, fulfillment_order_no = ?, target_warehouse_name = ?, updated_at = ? where id = ?",
                    now,
                    fulfillmentOrderNo,
                    support.s(omsOrder.get("warehouse_name")),
                    now,
                    id
            );
        }
        Map<String, Object> refreshed = support.requiredOne("select * from upstream_order where id = ?", "upstream order not found: " + id, id);
        return support.map(
                "source", support.map(
                        "id", refreshed.get("id"),
                        "upstreamOrderNo", support.s(refreshed.get("upstream_order_no")),
                        "externalNo", support.s(refreshed.get("external_no"))
                ),
                "result", support.map(
                        "upstreamOrderId", id,
                        "fulfillmentOrderNo", fulfillmentOrderNo,
                        "targetWarehouseName", support.s(refreshed.get("target_warehouse_name")),
                        "dispatchTime", support.formatDateTime(refreshed.get("dispatch_time")),
                        "lockResult", "库存锁定成功",
                        "wmsResult", "WMS 下发成功",
                        "decisionMode", "自动分仓",
                        "decisionTag", "规则引擎",
                        "decisionResult", "已根据库存与时效完成仓配决策",
                        "decisionSteps", List.of(
                                "命中分仓规则，目标仓库：" + support.s(refreshed.get("target_warehouse_name")),
                                "库存锁定完成",
                                "WMS 出库任务已创建"
                        ),
                        "status", "dispatched"
                )
        );
    }
}
