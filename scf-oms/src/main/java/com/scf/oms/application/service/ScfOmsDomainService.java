package com.scf.oms.application.service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ScfOmsDomainService {

    private final ScfFacadeSupport support;

    ScfOmsDomainService(ScfFacadeSupport support) {
        this.support = support;
    }

    Map<String, Object> omsOrders(Map<String, String> query) {
        List<Map<String, Object>> rows = support.jdbc().query(
                "select * from oms_order order by create_time desc",
                (rs, rowNum) -> support.map(
                        "id", rs.getLong("id"),
                        "orderNo", rs.getString("order_no"),
                        "externalNo", rs.getString("external_no"),
                        "status", rs.getInt("status"),
                        "statusText", rs.getString("status_text"),
                        "receiverName", rs.getString("receiver_name"),
                        "receiverPhone", rs.getString("receiver_phone"),
                        "warehouseId", rs.getString("warehouse_code"),
                        "warehouseName", rs.getString("warehouse_name"),
                        "logisticsProvider", rs.getString("logistics_provider"),
                        "logisticsProviderName", rs.getString("logistics_provider_name"),
                        "trackingNumber", rs.getString("tracking_number"),
                        "interceptStatus", rs.getString("intercept_status"),
                        "totalAmount", support.money(rs.getBigDecimal("total_amount")),
                        "createTime", support.format(rs.getTimestamp("create_time")),
                        "dispatchTime", support.format(rs.getTimestamp("dispatch_time")),
                        "outboundTime", support.format(rs.getTimestamp("outbound_time")),
                        "updateTime", support.format(rs.getTimestamp("update_time"))
                )
        ).stream()
                .filter(row -> support.contains(row, "orderNo", query.get("orderNo")))
                .filter(row -> support.contains(row, "externalNo", query.get("externalNo")))
                .filter(row -> support.contains(row, "receiverName", query.get("receiverName")))
                .filter(row -> support.contains(row, "receiverPhone", query.get("receiverPhone")))
                .filter(row -> support.equalsInt(row, "status", query.get("status")))
                .filter(row -> support.equalsValue(row, "warehouseId", query.get("warehouseId")))
                .filter(row -> support.equalsValue(row, "logisticsProvider", query.get("logisticsProvider")))
                .filter(row -> support.contains(row, "trackingNumber", query.get("trackingNumber")))
                .toList();
        Map<String, Object> result = support.page(rows, query);
        result.put("orders", result.get("list"));
        result.put("statusOptions", support.prependAll(support.orderStatusOptions()));
        result.put("warehouseOptions", support.prependAll(support.warehouseOptions()));
        result.put("logisticsProviders", support.prependAll(support.providerOptions()));
        return result;
    }

    Map<String, Object> omsWorkspace(Map<String, String> query) {
        Map<String, Object> orders = omsOrders(query);
        List<?> list = (List<?>) orders.get("list");
        Map<String, Object> details = new LinkedHashMap<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                Long id = Long.parseLong(String.valueOf(row.get("id")));
                details.put(String.valueOf(id), omsOrderDetail(id));
            }
        }
        return support.map(
                "orders", orders.get("list"),
                "list", orders.get("list"),
                "pageNo", orders.get("pageNo"),
                "pageSize", orders.get("pageSize"),
                "total", orders.get("total"),
                "statusOptions", orders.get("statusOptions"),
                "warehouseOptions", orders.get("warehouseOptions"),
                "logisticsProviders", orders.get("logisticsProviders"),
                "details", details,
                "defaults", support.map(
                        "orderNo", "",
                        "externalNo", "",
                        "receiverName", "",
                        "receiverPhone", "",
                        "warehouseId", "",
                        "logisticsProvider", "",
                        "status", "",
                        "trackingNumber", ""
                )
        );
    }

    Map<String, Object> omsOrderDetail(Long id) {
        Map<String, Object> base = support.requiredOne("select * from oms_order where id = ?", "oms order not found: " + id, id);
        String orderNo = support.s(base.get("order_no"));
        List<Map<String, Object>> items = support.jdbc().query(
                "select * from oms_order_item where order_no = ? order by id",
                (rs, rowNum) -> support.map(
                        "id", rs.getLong("id"),
                        "skuId", rs.getString("sku_code"),
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "tempLayer", rs.getString("temp_layer"),
                        "quantity", rs.getInt("quantity"),
                        "splitAmount", rs.getInt("quantity"),
                        "unitPrice", support.money(rs.getBigDecimal("unit_price")),
                        "amount", support.money(rs.getBigDecimal("amount")),
                        "weight", "1.00"
                ),
                orderNo
        );
        List<Map<String, Object>> logs = support.jdbc().query(
                "select * from oms_order_log where order_no = ? order by log_time desc, id desc",
                (rs, rowNum) -> support.map(
                        "id", rs.getLong("id"),
                        "time", support.format(rs.getTimestamp("log_time")),
                        "createTime", support.format(rs.getTimestamp("log_time")),
                        "nodeCode", rs.getString("node_code"),
                        "actionCode", rs.getString("action_code"),
                        "operatorName", rs.getString("operator_name"),
                        "remark", rs.getString("remark"),
                        "oldStatus", "",
                        "newStatus", support.statusTextFromAction(rs.getString("action_code"), support.s(base.get("status_text"))),
                        "oldStatusText", "",
                        "newStatusText", support.statusTextFromAction(rs.getString("action_code"), support.s(base.get("status_text")))
                ),
                orderNo
        );
        return support.map(
                "base", support.map(
                        "id", base.get("id"),
                        "orderNo", orderNo,
                        "externalNo", support.s(base.get("external_no")),
                        "parentId", base.get("parent_id"),
                        "status", support.n((Integer) base.get("status")),
                        "statusText", support.s(base.get("status_text")),
                        "receiverName", support.s(base.get("receiver_name")),
                        "receiverPhone", support.s(base.get("receiver_phone")),
                        "province", support.s(base.get("province")),
                        "city", support.s(base.get("city")),
                        "district", support.s(base.get("district")),
                        "detailAddress", support.s(base.get("detail_address")),
                        "warehouseId", support.s(base.get("warehouse_code")),
                        "warehouseName", support.s(base.get("warehouse_name")),
                        "logisticsProvider", support.s(base.get("logistics_provider")),
                        "logisticsProviderName", support.s(base.get("logistics_provider_name")),
                        "trackingNumber", support.s(base.get("tracking_number")),
                        "totalAmount", support.money((java.math.BigDecimal) base.get("total_amount")),
                        "routeReason", support.s(base.get("route_reason")),
                        "splitRemark", support.s(base.get("split_remark")),
                        "interceptStatus", support.s(base.get("intercept_status")),
                        "versionNo", support.n((Integer) base.get("version_no")),
                        "createTime", support.formatDateTime(base.get("create_time")),
                        "dispatchTime", support.formatDateTime(base.get("dispatch_time")),
                        "outboundTime", support.formatDateTime(base.get("outbound_time")),
                        "updateTime", support.formatDateTime(base.get("update_time"))
                ),
                "details", items,
                "logs", logs
        );
    }

    Map<String, Object> omsRules(Map<String, String> query) {
        List<Map<String, Object>> rows = support.jdbc().query(
                "select * from oms_rule order by priority_no asc, id asc",
                (rs, rowNum) -> mapRuleRow(rs.getLong("id"))
        ).stream()
                .filter(row -> support.contains(row, "ruleName", support.blankTo(query.get("ruleName"), query.get("keyword"))))
                .filter(row -> support.equalsValue(row, "status", query.get("status")))
                .filter(row -> support.equalsValue(row, "ruleType", query.get("ruleType")))
                .toList();
        return support.map(
                "records", rows,
                "options", support.map(
                        "ruleTypes", List.of(support.option("分仓规则", "warehouse_route")),
                        "warehouses", support.prependAll(support.warehouseOptions()),
                        "statuses", List.of(support.option("启用", "enabled"), support.option("停用", "disabled")),
                        "conditionFields", List.of(
                                support.map("label", "收货省份", "value", "receiverProvince"),
                                support.map("label", "收货城市", "value", "receiverCity"),
                                support.map("label", "温层", "value", "tempLayer"),
                                support.map("label", "配送时效", "value", "requestedDelivery")
                        ),
                        "operators", List.of(
                                support.option("等于", "="),
                                support.option("不等于", "!="),
                                support.option("包含", "contains")
                        )
                ),
                "defaultForm", support.map(
                        "ruleType", "warehouse_route",
                        "warehouse", "",
                        "status", "enabled",
                        "priority", 10
                )
        );
    }

    Map<String, Object> createOmsRule(Map<String, Object> req) {
        long id = support.nextId("oms_rule", 3000L);
        saveRule(id, req, true);
        return mapRuleRow(id);
    }

    Map<String, Object> updateOmsRule(Long id, Map<String, Object> req) {
        support.requiredOne("select id from oms_rule where id = ?", "rule not found: " + id, id);
        saveRule(id, req, false);
        return mapRuleRow(id);
    }

    Boolean deleteOmsRule(Long id) {
        support.jdbc().update("delete from oms_rule_condition where rule_id = ?", id);
        return support.jdbc().update("delete from oms_rule where id = ?", id) > 0;
    }

    Map<String, Object> splitMergeRequests(Map<String, String> query) {
        List<Map<String, Object>> rows = support.jdbc().query(
                "select * from split_merge_request order by created_at desc",
                (rs, rowNum) -> support.map(
                        "id", rs.getLong("id"),
                        "requestNo", rs.getString("request_no"),
                        "requestType", rs.getString("request_type"),
                        "strategy", rs.getString("strategy_code"),
                        "targetWarehouse", rs.getString("target_warehouse"),
                        "reason", rs.getString("reason"),
                        "status", rs.getString("status"),
                        "resultSummary", rs.getString("result_summary"),
                        "operator", rs.getString("operator_name"),
                        "createdAt", support.format(rs.getTimestamp("created_at")),
                        "processedAt", support.format(rs.getTimestamp("processed_at")),
                        "sourceOrderNos", support.jdbc().query(
                                "select source_order_no from split_merge_request_order_rel where request_no = ? order by id",
                                (r, idx) -> r.getString(1),
                                rs.getString("request_no")
                        )
                )
        ).stream()
                .filter(row -> support.equalsValue(row, "status", query.get("status")))
                .filter(row -> support.equalsValue(row, "requestType", query.get("requestType")))
                .filter(row -> support.contains(row, "requestNo", query.get("requestNo")))
                .filter(row -> support.contains(row, "targetWarehouse", query.get("targetWarehouse")))
                .toList();
        Map<String, Object> result = support.page(rows, query);
        result.put("records", result.get("list"));
        result.put("options", support.map(
                "statuses", support.prependAll(support.simpleOptions("pending", "待处理", "done", "已执行", "cancelled", "已取消")),
                "requestTypes", support.prependAll(support.simpleOptions("split", "拆单", "merge", "合单")),
                "splitStrategies", support.prependAll(support.simpleOptions("manual", "人工拆单", "split_by_temp", "按温层拆单", "by_quantity", "按数量拆单")),
                "mergeStrategies", support.prependAll(support.simpleOptions("manual", "人工合单", "merge_same_address", "同地址合单", "same_carrier", "同承运商合单")),
                "warehouses", support.prependAll(support.warehouseOptions()),
                "orderOptions", support.prependAll(support.jdbc().query(
                        "select order_no from oms_order order by create_time desc limit 50",
                        (rs, rowNum) -> support.option(rs.getString(1), rs.getString(1))
                ))
        ));
        result.put("defaultForm", support.map(
                "requestType", "split",
                "sourceOrderNos", List.of(),
                "strategy", "manual",
                "targetWarehouse", "",
                "reason", "",
                "operator", "system"
        ));
        return result;
    }

    Map<String, Object> createSplitMergeRequest(Map<String, Object> req) {
        long id = support.nextId("split_merge_request", 5000L);
        Timestamp now = support.now();
        String requestNo = "SM" + id;
        support.jdbc().update(
                "insert into split_merge_request (id, request_no, request_type, strategy_code, target_warehouse, reason, status, result_summary, operator_name, operation_source, created_at, processed_at) values (?, ?, ?, ?, ?, ?, 'pending', '', ?, ?, ?, null)",
                id,
                requestNo,
                support.blankTo(support.s(req.get("requestType")), "split"),
                support.blankTo(support.s(req.get("strategy")), "manual"),
                support.s(req.get("targetWarehouse")),
                support.blankTo(support.s(req.get("reason")), "manual request"),
                support.blankTo(support.s(req.get("operator")), "system"),
                support.blankTo(support.s(req.get("operationSource")), "manual"),
                now
        );
        Object sourceOrders = req.get("sourceOrderNos");
        if (sourceOrders instanceof List<?> list) {
            for (Object sourceOrder : list) {
                support.jdbc().update(
                        "insert into split_merge_request_order_rel (request_no, source_order_no, created_at) values (?, ?, ?)",
                        requestNo,
                        String.valueOf(sourceOrder),
                        now
                );
            }
        }
        return support.requiredOne("select * from split_merge_request where id = ?", "request not found: " + id, id);
    }

    Map<String, Object> executeSplitMergeRequest(Long id) {
        return support.updateSplitMergeStatus(id, "done", "request executed");
    }

    Map<String, Object> cancelSplitMergeRequest(Long id) {
        return support.updateSplitMergeStatus(id, "cancelled", "request cancelled");
    }

    Map<String, Object> omsDashboard() {
        int created = support.count("select count(1) from oms_order where status = 10");
        int routed = support.count("select count(1) from oms_order where status = 20");
        int dispatched = support.count("select count(1) from oms_order where status = 40");
        int shipped = support.count("select count(1) from oms_order where status = 50");
        int delivered = support.count("select count(1) from oms_order where status = 60");
        List<Map<String, Object>> orders = support.jdbc().query(
                "select * from oms_order order by update_time desc limit 10",
                (rs, rowNum) -> support.map(
                        "id", rs.getLong("id"),
                        "orderNo", rs.getString("order_no"),
                        "externalNo", rs.getString("external_no"),
                        "status", rs.getInt("status"),
                        "statusText", rs.getString("status_text"),
                        "receiverName", rs.getString("receiver_name"),
                        "receiverPhone", rs.getString("receiver_phone"),
                        "province", rs.getString("province"),
                        "city", rs.getString("city"),
                        "district", rs.getString("district"),
                        "detailAddress", rs.getString("detail_address"),
                        "warehouseId", rs.getString("warehouse_code"),
                        "warehouseName", rs.getString("warehouse_name"),
                        "logisticsProvider", rs.getString("logistics_provider"),
                        "logisticsProviderName", rs.getString("logistics_provider_name"),
                        "trackingNumber", rs.getString("tracking_number"),
                        "totalAmount", support.money(rs.getBigDecimal("total_amount")),
                        "createTime", support.format(rs.getTimestamp("create_time")),
                        "dispatchTime", support.format(rs.getTimestamp("dispatch_time")),
                        "outboundTime", support.format(rs.getTimestamp("outbound_time")),
                        "parentId", rs.getObject("parent_id"),
                        "routeSuccess", rs.getInt("status") >= 20,
                        "splitFlag", rs.getObject("parent_id") != null,
                        "interceptRequested", !"none".equalsIgnoreCase(rs.getString("intercept_status")),
                        "interceptSuccess", "intercepted".equalsIgnoreCase(rs.getString("intercept_status")),
                        "overdue", support.isOverdue(rs.getTimestamp("create_time"), rs.getInt("status")),
                        "exceptionType", support.findExceptionType(rs.getString("order_no")),
                        "agingHours", support.agingHours(rs.getTimestamp("create_time")),
                        "updateTime", support.format(rs.getTimestamp("update_time"))
                )
        );
        return support.map(
                "generatedAt", support.format(support.now()),
                "cards", List.of(
                        support.map("key", "created", "label", "新建单量", "value", String.valueOf(created)),
                        support.map("key", "routed", "label", "已分仓", "value", String.valueOf(routed)),
                        support.map("key", "dispatched", "label", "已下发", "value", String.valueOf(dispatched)),
                        support.map("key", "shipped", "label", "已出库", "value", String.valueOf(shipped)),
                        support.map("key", "delivered", "label", "已签收", "value", String.valueOf(delivered)),
                        support.map("key", "exceptionOpen", "label", "异常工单", "value", String.valueOf(support.count("select count(1) from oms_exception_ticket where status <> 'closed'")))
                ),
                "moduleStats", List.of(
                        support.map("module", "route", "label", "分仓命中数", "value", String.valueOf(support.count("select count(1) from oms_order where status >= 20"))),
                        support.map("module", "splitMerge", "label", "待处理拆合单", "value", String.valueOf(support.count("select count(1) from split_merge_request where status = 'pending'"))),
                        support.map("module", "splitFlag", "label", "拆分子单数", "value", String.valueOf(support.count("select count(1) from oms_order where parent_id is not null"))),
                        support.map("module", "aging", "label", "平均时效(小时)", "value", String.valueOf(support.averageAgingHours()))
                ),
                "orders", orders,
                "orderStatusSummary", support.orderStatusOptions(),
                "pendingSplitRequests", support.count("select count(1) from split_merge_request where status = 'pending'"),
                "filterOptions", support.map(
                        "dateRange", List.of(support.option("今天", "today"), support.option("近7天", "7d"), support.option("近30天", "30d"))
                )
        );
    }
