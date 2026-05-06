package com.scf.oms.application.service;

import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;
import com.scf.oms.interfaces.dto.OrderCreateReq;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ScfFacadeService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final FulfillmentOrderService fulfillmentOrderService;

    public ScfFacadeService(JdbcTemplate jdbcTemplate, FulfillmentOrderService fulfillmentOrderService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fulfillmentOrderService = fulfillmentOrderService;
    }

    public Map<String, Object> login(Map<String, Object> req) {
        String username = s(req.get("username"));
        String password = s(req.get("password"));
        Map<String, Object> user = one(
                "select * from scf_user where username = ? and password = ? and status = 1",
                username,
                password
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "username or password is invalid");
        }
        return authPayload(user);
    }

    @Transactional
    public Map<String, Object> register(Map<String, Object> req) {
        String username = require(s(req.get("username")), "username is required");
        String password = require(s(req.get("password")), "password is required");
        if (exists("select count(1) from scf_user where username = ?", username)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "username already exists: " + username);
        }
        long id = nextId("scf_user", 1000L);
        Timestamp now = now();
        String roleCode = blankTo(s(req.get("roleCode")), "operations");
        String roleName = blankTo(s(req.get("role")), blankTo(s(req.get("roleName")), roleCode));
        String displayName = blankTo(s(req.get("name")), blankTo(s(req.get("displayName")), username));
        jdbcTemplate.update(
                "insert into scf_user (id, user_code, username, password, display_name, role_code, role_name, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)",
                id,
                "user-" + id,
                username,
                password,
                displayName,
                roleCode,
                roleName,
                now,
                now
        );
        return currentUserById(id);
    }

    public Map<String, Object> currentUser() {
        Map<String, Object> user = one("select * from scf_user where status = 1 order by id limit 1");
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "active user not found");
        }
        return authPayload(user);
    }

    public List<Map<String, Object>> users() {
        return jdbcTemplate.query(
                "select * from scf_user where status = 1 order by id",
                (rs, rowNum) -> map(
                        "id", rs.getString("user_code"),
                        "username", rs.getString("username"),
                        "displayName", rs.getString("display_name"),
                        "roleCode", rs.getString("role_code"),
                        "roleName", rs.getString("role_name"),
                        "status", rs.getInt("status")
                )
        );
    }

    public Map<String, Object> navigation() {
        return map(
                "items", List.of(
                        navGroup("upstream", "订单接入", "上游订单与履约入口", List.of(
                                navItem("upstream-orders", "订单接入", "UP")
                        )),
                        navGroup("oms", "OMS", "履约工作台与规则中心", List.of(
                                navItem("oms-workspace", "履约工作台", "OW"),
                                navItem("oms-rules", "路由规则", "OR"),
                                navItem("oms-exceptions", "异常中心", "OE"),
                                navItem("oms-split-merge", "拆单合单", "SM"),
                                navItem("oms-dashboard", "OMS 看板", "OD")
                        )),
                        navGroup("isc", "ISC", "库存台账与预警", List.of(
                                navItem("isc-ledger", "库存台账", "IL"),
                                navItem("isc-adjustments", "库存调整", "IA"),
                                navItem("isc-alerts", "安全库存", "SA")
                        )),
                        navGroup("wms", "WMS", "波次、拣货、打包、出库", List.of(
                                navItem("wms-taskhall", "波次大厅", "TH"),
                                navItem("wms-picking", "拣货任务", "PK"),
                                navItem("wms-packing", "打包复核", "PA"),
                                navItem("wms-shipment", "出库交接", "SH")
                        )),
                        navGroup("lgs", "LGS", "物流商、在途交付与回传", List.of(
                                navItem("lgs-providers", "物流商管理", "LP"),
                                navItem("lgs-delivery", "在途交付", "LD"),
                                navItem("lgs-callback", "回传记录", "LC")
                        ))
                )
        );
    }

    public Map<String, Object> upstreamOrders(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from upstream_order order by create_time desc",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "upstreamOrderNo", rs.getString("upstream_order_no"),
                        "externalNo", rs.getString("external_no"),
                        "channel", rs.getString("channel_code"),
                        "channelCode", rs.getString("channel_code"),
                        "receiverName", rs.getString("receiver_name"),
                        "receiverPhone", rs.getString("receiver_phone"),
                        "province", rs.getString("province"),
                        "city", rs.getString("city"),
                        "district", rs.getString("district"),
                        "detailAddress", rs.getString("detail_address"),
                        "tempLayer", rs.getString("temp_layer"),
                        "requestedDelivery", rs.getString("requested_delivery"),
                        "totalAmount", money(rs.getBigDecimal("total_amount")),
                        "remark", rs.getString("remark"),
                        "status", rs.getString("status"),
                        "statusText", rs.getString("status_text"),
                        "dispatchTime", format(rs.getTimestamp("dispatch_time")),
                        "fulfillmentOrderNo", rs.getString("fulfillment_order_no"),
                        "targetWarehouseName", rs.getString("target_warehouse_name"),
                        "createTime", format(rs.getTimestamp("create_time"))
                )
        ).stream()
                .filter(row -> contains(row, "upstreamOrderNo", query.get("upstreamOrderNo")))
                .filter(row -> contains(row, "externalNo", query.get("externalNo")))
                .filter(row -> contains(row, "receiverName", query.get("receiverName")))
                .filter(row -> equalsValue(row, "channel", query.get("channel")))
                .filter(row -> equalsValue(row, "status", query.get("status")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "upstreamOrderNo", "externalNo", "receiverName"))
                .toList();
        Map<String, Object> result = page(rows, query);
        result.put("orders", result.get("list"));
        result.put("statusOptions", prependAll(simpleOptions("pending", "待下发", "dispatched", "已下发")));
        result.put("channelOptions", prependAll(jdbcTemplate.query(
                "select distinct channel_code from upstream_order where channel_code is not null and channel_code <> '' order by channel_code",
                (rs, rowNum) -> option(channelLabel(rs.getString(1)), rs.getString(1))
        )));
        result.put("defaults", map(
                "upstreamOrderNo", "",
                "externalNo", "",
                "receiverName", "",
                "channel", "",
                "status", ""
        ));
        return result;
    }

    @Transactional
    public Map<String, Object> dispatchUpstreamOrder(Long id) {
        Map<String, Object> upstream = requiredOne("select * from upstream_order where id = ?", "upstream order not found: " + id, id);
        String fulfillmentOrderNo = s(upstream.get("fulfillment_order_no"));
        if (blank(fulfillmentOrderNo)) {
            OrderCreateReq req = new OrderCreateReq();
            req.setExternalOrderId(s(upstream.get("external_no")));
            req.setReceiverName(s(upstream.get("receiver_name")));
            req.setReceiverPhone(s(upstream.get("receiver_phone")));
            req.setProvince(s(upstream.get("province")));
            req.setCity(s(upstream.get("city")));
            req.setDistrict(s(upstream.get("district")));
            req.setDetailAddress(s(upstream.get("detail_address")));
            req.setSkuList(jdbcTemplate.query(
                    "select * from upstream_order_item where upstream_order_no = ? order by id",
                    (rs, rowNum) -> {
                        OrderCreateReq.SkuItem item = new OrderCreateReq.SkuItem();
                        item.setSkuCode(rs.getString("sku_code"));
                        item.setSkuName(rs.getString("sku_name"));
                        item.setQuantity(rs.getInt("quantity"));
                        item.setPrice(rs.getBigDecimal("unit_price"));
                        return item;
                    },
                    s(upstream.get("upstream_order_no"))
            ));
            fulfillmentOrderNo = fulfillmentOrderService.createOrder(req);
            Map<String, Object> omsOrder = requiredOne("select warehouse_name from oms_order where order_no = ?", "oms order not found: " + fulfillmentOrderNo, fulfillmentOrderNo);
            Timestamp now = now();
            jdbcTemplate.update(
                    "update upstream_order set status = 'dispatched', status_text = 'Dispatched', dispatch_time = ?, fulfillment_order_no = ?, target_warehouse_name = ?, updated_at = ? where id = ?",
                    now,
                    fulfillmentOrderNo,
                    s(omsOrder.get("warehouse_name")),
                    now,
                    id
            );
        }
        Map<String, Object> refreshed = requiredOne("select * from upstream_order where id = ?", "upstream order not found: " + id, id);
        return map(
                "source", map(
                        "id", refreshed.get("id"),
                        "upstreamOrderNo", s(refreshed.get("upstream_order_no")),
                        "externalNo", s(refreshed.get("external_no"))
                ),
                "result", map(
                        "upstreamOrderId", id,
                        "fulfillmentOrderNo", fulfillmentOrderNo,
                        "targetWarehouseName", s(refreshed.get("target_warehouse_name")),
                        "dispatchTime", formatDateTime(refreshed.get("dispatch_time")),
                        "lockResult", "库存锁定成功",
                        "wmsResult", "WMS 下发成功",
                        "decisionMode", "自动分仓",
                        "decisionTag", "规则引擎",
                        "decisionResult", "已根据库存与时效完成仓配决策",
                        "decisionSteps", List.of(
                                "命中分仓规则，目标仓库：" + s(refreshed.get("target_warehouse_name")),
                                "库存锁定完成",
                                "WMS 出库任务已创建"
                        ),
                        "status", "dispatched"
                )
        );
    }

    public Map<String, Object> omsOrders(Map<String, String> query) {
        List<Map<String, Object>> rows = orderRows();
        List<Map<String, Object>> filtered = rows.stream()
                .filter(row -> contains(row, "orderNo", query.get("orderNo")))
                .filter(row -> contains(row, "externalNo", query.get("externalNo")))
                .filter(row -> contains(row, "receiverName", query.get("receiverName")))
                .filter(row -> contains(row, "receiverPhone", query.get("receiverPhone")))
                .filter(row -> equalsInt(row, "status", query.get("status")))
                .filter(row -> equalsValue(row, "warehouseId", query.get("warehouseId")))
                .filter(row -> equalsValue(row, "logisticsProvider", query.get("logisticsProvider")))
                .filter(row -> contains(row, "trackingNumber", query.get("trackingNumber")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "orderNo", "externalNo", "receiverName", "trackingNumber"))
                .toList();
        Map<String, Object> result = page(filtered, query);
        result.put("orders", result.get("list"));
        result.put("statusOptions", prependAll(orderStatusOptions()));
        result.put("warehouseOptions", prependAll(warehouseOptions()));
        result.put("logisticsProviders", prependAll(providerOptions()));
        return result;
    }

    public Map<String, Object> omsWorkspace(Map<String, String> query) {
        Map<String, Object> orders = omsOrders(query);
        List<?> list = (List<?>) orders.get("list");
        Map<String, Object> details = new LinkedHashMap<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                Long id = Long.parseLong(String.valueOf(row.get("id")));
                details.put(String.valueOf(id), omsOrderDetail(id));
            }
        }
        return map(
                "orders", orders.get("list"),
                "list", orders.get("list"),
                "pageNo", orders.get("pageNo"),
                "pageSize", orders.get("pageSize"),
                "total", orders.get("total"),
                "statusOptions", orders.get("statusOptions"),
                "warehouseOptions", orders.get("warehouseOptions"),
                "logisticsProviders", orders.get("logisticsProviders"),
                "details", details,
                "defaults", map(
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

    public Map<String, Object> omsOrderDetail(Long id) {
        Map<String, Object> base = requiredOne("select * from oms_order where id = ?", "oms order not found: " + id, id);
        String orderNo = s(base.get("order_no"));
        List<Map<String, Object>> items = jdbcTemplate.query(
                "select * from oms_order_item where order_no = ? order by id",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "skuId", rs.getString("sku_code"),
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "tempLayer", rs.getString("temp_layer"),
                        "quantity", rs.getInt("quantity"),
                        "splitAmount", rs.getInt("quantity"),
                        "unitPrice", money(rs.getBigDecimal("unit_price")),
                        "amount", money(rs.getBigDecimal("amount")),
                        "weight", "1.00"
                ),
                orderNo
        );
        List<Map<String, Object>> logs = jdbcTemplate.query(
                "select * from oms_order_log where order_no = ? order by log_time desc, id desc",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "time", format(rs.getTimestamp("log_time")),
                        "createTime", format(rs.getTimestamp("log_time")),
                        "nodeCode", rs.getString("node_code"),
                        "actionCode", rs.getString("action_code"),
                        "operatorName", rs.getString("operator_name"),
                        "remark", rs.getString("remark"),
                        "oldStatus", "",
                        "newStatus", statusTextFromAction(rs.getString("action_code"), s(base.get("status_text"))),
                        "oldStatusText", "",
                        "newStatusText", statusTextFromAction(rs.getString("action_code"), s(base.get("status_text")))
                ),
                orderNo
        );
        return map(
                "base", map(
                        "id", base.get("id"),
                        "orderNo", orderNo,
                        "externalNo", s(base.get("external_no")),
                        "parentId", base.get("parent_id"),
                        "status", n((Integer) base.get("status")),
                        "statusText", s(base.get("status_text")),
                        "receiverName", s(base.get("receiver_name")),
                        "receiverPhone", s(base.get("receiver_phone")),
                        "province", s(base.get("province")),
                        "city", s(base.get("city")),
                        "district", s(base.get("district")),
                        "detailAddress", s(base.get("detail_address")),
                        "warehouseId", s(base.get("warehouse_code")),
                        "warehouseName", s(base.get("warehouse_name")),
                        "logisticsProvider", s(base.get("logistics_provider")),
                        "logisticsProviderName", s(base.get("logistics_provider_name")),
                        "trackingNumber", s(base.get("tracking_number")),
                        "totalAmount", money((BigDecimal) base.get("total_amount")),
                        "routeReason", s(base.get("route_reason")),
                        "splitRemark", s(base.get("split_remark")),
                        "interceptStatus", s(base.get("intercept_status")),
                        "versionNo", n((Integer) base.get("version_no")),
                        "createTime", formatDateTime(base.get("create_time")),
                        "dispatchTime", formatDateTime(base.get("dispatch_time")),
                        "outboundTime", formatDateTime(base.get("outbound_time")),
                        "updateTime", formatDateTime(base.get("update_time"))
                ),
                "details", items,
                "logs", logs
        );
    }

    public Map<String, Object> omsRules(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from oms_rule order by priority_no asc, id asc",
                (rs, rowNum) -> mapRuleRow(rs.getLong("id"))
        ).stream()
                .filter(row -> contains(row, "ruleName", blankTo(query.get("ruleName"), query.get("keyword"))))
                .filter(row -> equalsValue(row, "status", query.get("status")))
                .filter(row -> equalsValue(row, "ruleType", query.get("ruleType")))
                .toList();
        return map(
                "records", rows,
                "options", map(
                        "ruleTypes", List.of(option("分仓规则", "warehouse_route")),
                        "warehouses", prependAll(jdbcTemplate.query(
                                "select warehouse_name from warehouse where status = 'enabled' order by id",
                                (rs, rowNum) -> option(rs.getString(1), rs.getString(1))
                        )),
                        "statuses", List.of("启用", "停用"),
                        "conditionFields", List.of(
                                map("label", "收货省份", "value", "receiverProvince"),
                                map("label", "收货城市", "value", "receiverCity"),
                                map("label", "温层", "value", "tempLayer"),
                                map("label", "履约时效", "value", "requestedDelivery")
                        ),
                        "operators", List.of("=", "!=", "contains"),
                        "statuses", List.of(option("\u542f\u7528", "enabled"), option("\u505c\u7528", "disabled")),
                        "conditionFields", List.of(
                                map("label", "\u6536\u8d27\u7701\u4efd", "value", "receiverProvince"),
                                map("label", "\u6536\u8d27\u57ce\u5e02", "value", "receiverCity"),
                                map("label", "\u6e29\u5c42", "value", "tempLayer"),
                                map("label", "\u914d\u9001\u65f6\u6548", "value", "requestedDelivery")
                        ),
                        "operators", List.of(option("\u7b49\u4e8e", "="), option("\u4e0d\u7b49\u4e8e", "!="), option("\u5305\u542b", "contains"))
                )
        );
    }

    @Transactional
    public Map<String, Object> createOmsRule(Map<String, Object> req) {
        long id = nextId("oms_rule", 3000L);
        saveRule(id, req, true);
        return mapRuleRow(id);
    }

    @Transactional
    public Map<String, Object> updateOmsRule(Long id, Map<String, Object> req) {
        requiredOne("select id from oms_rule where id = ?", "rule not found: " + id, id);
        saveRule(id, req, false);
        return mapRuleRow(id);
    }

    @Transactional
    public Boolean deleteOmsRule(Long id) {
        jdbcTemplate.update("delete from oms_rule_condition where rule_id = ?", id);
        return jdbcTemplate.update("delete from oms_rule where id = ?", id) > 0;
    }

    public Map<String, Object> splitMergeRequests(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from split_merge_request order by created_at desc",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "requestNo", rs.getString("request_no"),
                        "requestType", rs.getString("request_type"),
                        "strategyCode", rs.getString("strategy_code"),
                        "targetWarehouse", rs.getString("target_warehouse"),
                        "reason", rs.getString("reason"),
                        "status", rs.getString("status"),
                        "resultSummary", rs.getString("result_summary"),
                        "operatorName", rs.getString("operator_name"),
                        "operationSource", rs.getString("operation_source"),
                        "createdAt", format(rs.getTimestamp("created_at")),
                        "processedAt", format(rs.getTimestamp("processed_at")),
                        "sourceOrderNos", jdbcTemplate.query(
                                "select source_order_no from split_merge_request_order_rel where request_no = ? order by id",
                                (r, idx) -> r.getString(1),
                                rs.getString("request_no")
                        ),
                        "strategy", rs.getString("strategy_code"),
                        "operator", rs.getString("operator_name")
                )
        ).stream()
                .filter(row -> equalsValue(row, "status", query.get("status")))
                .filter(row -> equalsValue(row, "requestType", query.get("requestType")))
                .filter(row -> contains(row, "requestNo", query.get("requestNo")))
                .filter(row -> contains(row, "targetWarehouse", query.get("targetWarehouse")))
                .toList();
        Map<String, Object> result = page(rows, query);
        result.put("records", result.get("list"));
        result.put("options", map(
                "statuses", prependAll(simpleOptions("pending", "\u5f85\u5904\u7406", "done", "\u5df2\u6267\u884c", "cancelled", "\u5df2\u53d6\u6d88")),
                "requestTypes", prependAll(simpleOptions("split", "\u62c6\u5355", "merge", "\u5408\u5355")),
                "splitStrategies", prependAll(simpleOptions("manual", "\u4eba\u5de5\u62c6\u5355", "split_by_temp", "\u6309\u6e29\u5c42\u62c6\u5355", "by_quantity", "\u6309\u6570\u91cf\u62c6\u5355")),
                "mergeStrategies", prependAll(simpleOptions("manual", "\u4eba\u5de5\u5408\u5355", "merge_same_address", "\u540c\u5730\u5740\u5408\u5355", "same_carrier", "\u540c\u627f\u8fd0\u5546\u5408\u5355")),
                "warehouses", prependAll(warehouseOptions()),
                "orderOptions", prependAll(jdbcTemplate.query(
                        "select order_no from oms_order order by create_time desc limit 50",
                        (rs, rowNum) -> option(rs.getString(1), rs.getString(1))
                ))
        ));
        result.put("defaultForm", map(
                "requestType", "split",
                "sourceOrderNos", List.of(),
                "strategy", "manual",
                "targetWarehouse", "",
                "reason", "",
                "operator", "system"
        ));
        return result;
    }

    @Transactional
    public Map<String, Object> createSplitMergeRequest(Map<String, Object> req) {
        long id = nextId("split_merge_request", 5000L);
        Timestamp now = now();
        String requestNo = "SM" + id;
        jdbcTemplate.update(
                "insert into split_merge_request (id, request_no, request_type, strategy_code, target_warehouse, reason, status, result_summary, operator_name, operation_source, created_at, processed_at) values (?, ?, ?, ?, ?, ?, 'pending', '', ?, ?, ?, null)",
                id,
                requestNo,
                blankTo(s(req.get("requestType")), "split"),
                blankTo(s(req.get("strategy")), blankTo(s(req.get("strategyCode")), "manual")),
                s(req.get("targetWarehouse")),
                blankTo(s(req.get("reason")), "manual request"),
                blankTo(s(req.get("operator")), blankTo(s(req.get("operatorName")), "system")),
                blankTo(s(req.get("operationSource")), "manual"),
                now
        );
        Object sourceOrders = req.containsKey("sourceOrderNos") ? req.get("sourceOrderNos") : req.get("sourceOrders");
        if (sourceOrders instanceof List<?> list) {
            for (Object sourceOrder : list) {
                jdbcTemplate.update(
                        "insert into split_merge_request_order_rel (request_no, source_order_no, created_at) values (?, ?, ?)",
                        requestNo,
                        String.valueOf(sourceOrder),
                        now
                );
            }
        }
        return requiredOne("select * from split_merge_request where id = ?", "request not found: " + id, id);
    }

    @Transactional
    public Map<String, Object> executeSplitMergeRequest(Long id) {
        return updateSplitMergeStatus(id, "done", "request executed");
    }

    @Transactional
    public Map<String, Object> cancelSplitMergeRequest(Long id) {
        return updateSplitMergeStatus(id, "cancelled", "request cancelled");
    }

    public Map<String, Object> omsDashboard() {
        int created = count("select count(1) from oms_order where status = 10");
        int routed = count("select count(1) from oms_order where status = 20");
        int dispatched = count("select count(1) from oms_order where status = 40");
        int shipped = count("select count(1) from oms_order where status = 50");
        int delivered = count("select count(1) from oms_order where status = 60");
        List<Map<String, Object>> orders = jdbcTemplate.query(
                "select * from oms_order order by update_time desc limit 10",
                (rs, rowNum) -> map(
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
                        "totalAmount", money(rs.getBigDecimal("total_amount")),
                        "createTime", format(rs.getTimestamp("create_time")),
                        "dispatchTime", format(rs.getTimestamp("dispatch_time")),
                        "outboundTime", format(rs.getTimestamp("outbound_time")),
                        "parentId", rs.getObject("parent_id"),
                        "routeSuccess", rs.getInt("status") >= 20,
                        "splitFlag", rs.getObject("parent_id") != null,
                        "interceptRequested", !"none".equalsIgnoreCase(rs.getString("intercept_status")),
                        "interceptSuccess", "intercepted".equalsIgnoreCase(rs.getString("intercept_status")),
                        "overdue", isOverdue(rs.getTimestamp("create_time"), rs.getInt("status")),
                        "exceptionType", findExceptionType(rs.getString("order_no")),
                        "agingHours", agingHours(rs.getTimestamp("create_time")),
                        "updateTime", format(rs.getTimestamp("update_time"))
                )
        );
        int pendingSplitRequests = count("select count(1) from split_merge_request where status = 'pending'");
        int routeSuccess = count("select count(1) from oms_order where status >= 20");
        int splitFlag = count("select count(1) from oms_order where parent_id is not null");
        int exceptionOpen = count("select count(1) from oms_exception_ticket where status <> 'closed'");
        return map(
                "generatedAt", format(now()),
                "cards", List.of(
                        map("key", "created", "label", "\u65b0\u5efa\u5355\u91cf", "value", String.valueOf(created)),
                        map("key", "routed", "label", "\u5df2\u5206\u4ed3", "value", String.valueOf(routed)),
                        map("key", "dispatched", "label", "\u5df2\u4e0b\u53d1", "value", String.valueOf(dispatched)),
                        map("key", "shipped", "label", "\u5df2\u51fa\u5e93", "value", String.valueOf(shipped)),
                        map("key", "delivered", "label", "\u5df2\u7b7e\u6536", "value", String.valueOf(delivered)),
                        map("key", "exceptionOpen", "label", "\u5f02\u5e38\u5de5\u5355", "value", String.valueOf(exceptionOpen))
                ),
                "moduleStats", List.of(
                        map("module", "route", "label", "\u5206\u4ed3\u547d\u4e2d\u6570", "value", String.valueOf(routeSuccess)),
                        map("module", "splitMerge", "label", "\u5f85\u5904\u7406\u62c6\u5408\u5355", "value", String.valueOf(pendingSplitRequests)),
                        map("module", "splitFlag", "label", "\u62c6\u5206\u5b50\u5355\u6570", "value", String.valueOf(splitFlag)),
                        map("module", "aging", "label", "\u5e73\u5747\u65f6\u6548(\u5c0f\u65f6)", "value", String.valueOf(averageAgingHours()))
                ),
                "orders", orders,
                "filterOptions", map(
                        "statuses", prependAll(orderStatusOptions()),
                        "warehouses", prependAll(warehouseOptions()),
                        "logisticsProviders", prependAll(providerOptions()),
                        "dateRange", List.of(option("\u4eca\u5929", "today"), option("\u8fd17\u5929", "7d"), option("\u8fd130\u5929", "30d"))
                )
        );
    }

    public Map<String, Object> omsExceptions(Map<String, String> query) {
        List<Map<String, Object>> tickets = jdbcTemplate.query(
                "select * from oms_exception_ticket order by created_at desc",
                (rs, rowNum) -> mapExceptionTicketSummary(requiredOne("select * from oms_exception_ticket where id = ?", "exception not found: " + rs.getLong("id"), rs.getLong("id")))
        ).stream()
                .filter(row -> equalsValue(row, "status", query.get("status")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "ticketNo", "orderNo", "reason"))
                .toList();
        int total = tickets.size();
        long success = tickets.stream().filter(ticket -> "Intercept Success".equals(ticket.get("result"))).count();
        long failed = tickets.stream().filter(ticket -> "Intercept Failed".equals(ticket.get("result"))).count();
        return map(
                "stats", List.of(
                        map("label", "\u5f02\u5e38\u5de5\u5355\u6570", "value", String.valueOf(total), "tip", "\u5f53\u524d OMS/WMS/LGS \u5f02\u5e38\u603b\u91cf"),
                        map("label", "\u6210\u529f\u5904\u7406", "value", String.valueOf(success), "tip", "\u5df2\u5b8c\u6210\u8865\u507f\u6216\u5173\u95ed"),
                        map("label", "\u5f85\u4eba\u5de5\u8ddf\u8fdb", "value", String.valueOf(failed), "tip", "\u4ecd\u9700\u4eba\u5de5\u5904\u7406"),
                        map("label", "\u5904\u7406\u6210\u529f\u7387", "value", total == 0 ? "0.0%" : String.format("%.1f%%", (success * 100.0) / total), "tip", "\u5f53\u524d\u5f02\u5e38\u5904\u7406\u8f6c\u5316")
                ),
                "tickets", tickets,
                "statusOptions", prependAll(simpleOptions("open", "\u5f85\u5904\u7406", "processing", "\u5904\u7406\u4e2d", "closed", "\u5df2\u5173\u95ed", "failed", "\u5904\u7406\u5931\u8d25")),
                "actionOptions", List.of(
                        option("\u91ca\u653e\u5e93\u5b58", "releaseInventory"),
                        option("\u56de\u5199 OMS \u72b6\u6001", "rewriteOmsStatus"),
                        option("\u751f\u6210\u8865\u507f\u65e5\u5fd7", "generateCompensationLog")
                )
        );
    }

    public Map<String, Object> omsExceptionDetail(Long id) {
        Map<String, Object> ticket = requiredOne("select * from oms_exception_ticket where id = ?", "exception not found: " + id, id);
        Map<String, Object> detail = mapExceptionTicketSummary(ticket);
        List<Map<String, Object>> actionLogs = jdbcTemplate.query(
                "select * from oms_exception_action_log where exception_id = ? order by created_at desc, id desc",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "actionCode", rs.getString("action_code"),
                        "actionResult", rs.getString("action_result"),
                        "remark", rs.getString("remark"),
                        "operatorName", rs.getString("operator_name"),
                        "createTime", format(rs.getTimestamp("created_at"))
                ),
                id
        );
        detail.put("monitorSteps", buildMonitorSteps(ticket));
        detail.put("availableActions", buildExceptionActions(ticket, actionLogs));
        detail.put("compensationLogs", actionLogs.stream()
                .filter(log -> "compensation_log".equals(log.get("actionCode")))
                .map(log -> map("id", log.get("id"), "time", log.get("createTime"), "text", blankTo(s(log.get("remark")), "Compensation log generated")))
                .toList());
        return detail;
    }

    @Transactional
    public Boolean releaseOmsExceptionInventory(Long id) {
        Map<String, Object> ticket = requiredOne("select * from oms_exception_ticket where id = ?", "exception not found: " + id, id);
        String orderNo = s(ticket.get("order_no"));
        Map<String, Object> order = requiredOne("select warehouse_code from oms_order where order_no = ?", "oms order not found: " + orderNo, orderNo);
        List<Map<String, Object>> items = jdbcTemplate.query(
                "select sku_code, quantity from oms_order_item where order_no = ?",
                (rs, rowNum) -> map("skuCode", rs.getString("sku_code"), "quantity", rs.getInt("quantity")),
                orderNo
        );
        for (Map<String, Object> item : items) {
            jdbcTemplate.update(
                    "update stock_inventory set available_quantity = available_quantity + ?, locked_quantity = greatest(locked_quantity - ?, 0) where warehouse_code = ? and sku_code = ?",
                    item.get("quantity"),
                    item.get("quantity"),
                    s(order.get("warehouse_code")),
                    s(item.get("skuCode"))
            );
        }
        updateExceptionStatus(id, "processing", "inventory released", "release_inventory");
        return true;
    }

    @Transactional
    public Boolean rewriteOmsExceptionStatus(Long id) {
        Map<String, Object> ticket = requiredOne("select * from oms_exception_ticket where id = ?", "exception not found: " + id, id);
        jdbcTemplate.update(
                "update oms_order set status = 20, status_text = 'Routed', update_time = ? where order_no = ?",
                now(),
                s(ticket.get("order_no"))
        );
        updateExceptionStatus(id, "closed", "oms status rewritten", "rewrite_status");
        return true;
    }

    @Transactional
    public Map<String, Object> generateOmsExceptionCompensationLog(Long id) {
        Timestamp current = now();
        updateExceptionStatus(id, "processing", "compensation log generated", "compensation_log", current, "Compensation log generated");
        return map("id", current.getTime(), "time", format(current), "text", "Compensation log generated");
    }

    public Map<String, Object> simulateOmsDispatch(Map<String, Object> req) {
        String channel = blankTo(s(req.get("channel")), "Mini Program");
        String externalNo = blankTo(s(req.get("externalNo")), "SO202604050001");
        String province = blankTo(s(req.get("province")), "Hubei");
        String city = blankTo(s(req.get("city")), "Wuhan");
        String tempLayer = blankTo(s(req.get("tempLayer")), "Ambient");
        String requestedDelivery = blankTo(s(req.get("requestedDelivery")), "Next Day");
        String manualWarehouseCode = s(req.get("manualWarehouseCode"));
        List<Map<String, Object>> candidates = buildSimulationCandidates(province, city, tempLayer, requestedDelivery);
        Map<String, Object> systemSelected = candidates.get(0);
        Map<String, Object> selectedWarehouse = blank(manualWarehouseCode)
                ? systemSelected
                : candidates.stream().filter(item -> manualWarehouseCode.equals(item.get("code"))).findFirst().orElse(systemSelected);
        boolean manualMode = !blank(manualWarehouseCode);
        String selectedName = s(selectedWarehouse.get("name"));
        Timestamp baseTime = Timestamp.valueOf(LocalDateTime.of(2026, 4, 5, 9, 0));
        return map(
                "orderNo", "FO" + digits(externalNo, "2026040501"),
                "flowStatus", "Dispatched to WMS",
                "decisionMode", manualMode ? "Manual Decision" : "System Decision",
                "decisionSummary", manualMode ? "Operator forced " + selectedName : "System selected " + selectedName,
                "latestLog", manualMode ? "Manual warehouse override has been replayed to the dispatch flow." : "System completed warehouse scoring and WMS dispatch.",
                "selectedWarehouse", map("code", selectedWarehouse.get("code"), "name", selectedName, "reason", firstString((List<?>) selectedWarehouse.get("reasons"))),
                "systemSelectedCode", systemSelected.get("code"),
                "candidates", candidates,
                "ruleResults", List.of(
                        map("id", "rule-1", "name", "Region Priority", "description", "Prefer nearest warehouse in the same city.", "priority", 10, "hit", city.equalsIgnoreCase(s(systemSelected.get("city"))), "result", "Applied to city-level scoring."),
                        map("id", "rule-2", "name", "Temperature Layer", "description", "Cold chain orders require cold-chain enabled warehouses.", "priority", 20, "hit", "Cold Chain".equalsIgnoreCase(tempLayer), "result", "Temperature constraints were checked."),
                        map("id", "rule-3", "name", "Stock and Capacity", "description", "Use stock and capacity as the main balancing factors.", "priority", 30, "hit", true, "result", "Scoring model completed.")
                ),
                "documents", List.of(
                        map("id", "doc-1", "owner", "OMS", "title", "Fulfillment Order", "status", "Created", "detail", "OMS accepted and created the fulfillment order.", "state", "done"),
                        map("id", "doc-2", "owner", "ISC", "title", "Inventory Lock", "status", "Locked", "detail", "Inventory locked in " + selectedName + ".", "state", "done"),
                        map("id", "doc-3", "owner", "WMS", "title", "Outbound Order", "status", "Dispatched", "detail", "Outbound order pushed to " + selectedName + ".", "state", "done"),
                        map("id", "doc-4", "owner", "LGS", "title", "Waybill", "status", "Pending", "detail", "Waiting for package data from WMS.", "state", "current")
                ),
                "logs", List.of(
                        map("id", "log-1", "title", "Order accepted", "remark", "OMS accepted the source order.", "operator", "SYSTEM", "time", format(addMinutes(baseTime, 0))),
                        map("id", "log-2", "title", manualMode ? "Manual override" : "Warehouse scored", "remark", manualMode ? "Operator selected " + selectedName + "." : "System selected " + selectedName + ".", "operator", manualMode ? "OPERATOR" : "OMS ENGINE", "time", format(addMinutes(baseTime, 2))),
                        map("id", "log-3", "title", "Inventory locked", "remark", "ISC lock completed successfully.", "operator", "ISC", "time", format(addMinutes(baseTime, 4))),
                        map("id", "log-4", "title", "WMS dispatched", "remark", "OMS pushed outbound order to WMS.", "operator", "OMS", "time", format(addMinutes(baseTime, 6)))
                ),
                "decisionSteps", List.of(
                        map("index", "01", "title", "Identify constraints", "detail", "Temp " + tempLayer + ", delivery " + requestedDelivery + ", city " + city + "."),
                        map("index", "02", "title", "Score warehouses", "detail", "Evaluated " + candidates.size() + " warehouse candidates."),
                        map("index", "03", "title", manualMode ? "Apply manual override" : "Output system result", "detail", manualMode ? "Manual warehouse " + selectedName + " replaced system selection." : "System output is " + selectedName + ".")
                ),
                "manualDecisionText", manualMode ? "Current warehouse is manually forced to " + selectedName + "." : "No manual override has been applied.",
                "manualLogs", manualMode
                        ? List.of(map("id", "manual-1", "title", "Manual override", "remark", "Warehouse switched to " + selectedName + ".", "time", format(addMinutes(baseTime, 3))))
                        : List.of(map("id", "manual-0", "title", "No manual action", "remark", "System decision is currently active.", "time", format(addMinutes(baseTime, 3)))),
                "request", map("channel", channel, "externalNo", externalNo)
        );
    }

    public Map<String, Object> lgsProviders() {
        return lgsProviders(Map.of());
    }

    public Map<String, Object> lgsProviders(Map<String, String> query) {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "select * from lgs_provider order by priority_no asc, id asc",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "providerCode", rs.getString("provider_code"),
                        "providerName", rs.getString("provider_name"),
                        "serviceScope", rs.getString("service_scope"),
                        "contactName", rs.getString("contact_name"),
                        "contactPhone", rs.getString("contact_phone"),
                        "priority", rs.getInt("priority_no"),
                        "status", rs.getString("status"),
                        "slaHours", rs.getInt("sla_hours"),
                        "baseFee", money(rs.getBigDecimal("base_fee")),
                        "feePerKg", money(rs.getBigDecimal("fee_per_kg")),
                        "apiEndpoint", rs.getString("api_endpoint"),
                        "remark", rs.getString("remark"),
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return map(
                "stats", List.of(
                        map("label", "\u5df2\u63a5\u5165\u7269\u6d41\u5546", "value", String.valueOf(list.size())),
                        map("label", "\u542f\u7528\u4e2d", "value", String.valueOf(list.stream().filter(item -> "enabled".equalsIgnoreCase(s(item.get("status")))).count()))
                ),
                "carriers", list,
                "apiKeys", list.stream().map(item -> map(
                        "providerCode", item.get("providerCode"),
                        "providerName", item.get("providerName"),
                        "apiEndpoint", item.get("apiEndpoint")
                )).toList(),
                "formOptions", map(
                        "types", List.of(option("\u5feb\u9012", "express"), option("\u51b7\u94fe", "cold_chain")),
                        "serviceScopes", List.of(option("\u5168\u56fd", "nationwide"), option("\u540c\u57ce", "same_city"), option("\u51b7\u94fe", "cold_chain"))
                ),
                "defaultForm", map(
                        "code", "",
                        "name", "",
                        "coverage", "nationwide",
                        "firstWeight", "10.00",
                        "extraWeight", "1.00",
                        "serviceScopes", List.of(),
                        "type", "express",
                        "dispatchPriority", 10
                )
        );
    }

    @Transactional
    public Map<String, Object> saveLgsProvider(Map<String, Object> req) {
        String code = blankTo(s(req.get("providerCode")), "");
        Timestamp now = now();
        if (blank(code) || !exists("select count(1) from lgs_provider where provider_code = ?", code)) {
            code = require(blankTo(code, s(req.get("code"))), "providerCode is required");
            jdbcTemplate.update(
                    "insert into lgs_provider (provider_code, provider_name, service_scope, contact_name, contact_phone, priority_no, status, sla_hours, base_fee, fee_per_kg, api_endpoint, remark, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    code,
                    require(s(req.get("providerName")), "providerName is required"),
                    blankTo(s(req.get("serviceScope")), "nationwide"),
                    s(req.get("contactName")),
                    s(req.get("contactPhone")),
                    intOr(req.get("priority"), 10),
                    blankTo(s(req.get("status")), "enabled"),
                    intOr(req.get("slaHours"), 48),
                    decimal(req.get("baseFee"), BigDecimal.TEN),
                    decimal(req.get("feePerKg"), BigDecimal.ONE),
                    s(req.get("apiEndpoint")),
                    s(req.get("remark")),
                    now,
                    now
            );
        } else {
            jdbcTemplate.update(
                    "update lgs_provider set provider_name = ?, service_scope = ?, contact_name = ?, contact_phone = ?, priority_no = ?, status = ?, sla_hours = ?, base_fee = ?, fee_per_kg = ?, api_endpoint = ?, remark = ?, updated_at = ? where provider_code = ?",
                    require(s(req.get("providerName")), "providerName is required"),
                    blankTo(s(req.get("serviceScope")), "nationwide"),
                    s(req.get("contactName")),
                    s(req.get("contactPhone")),
                    intOr(req.get("priority"), 10),
                    blankTo(s(req.get("status")), "enabled"),
                    intOr(req.get("slaHours"), 48),
                    decimal(req.get("baseFee"), BigDecimal.TEN),
                    decimal(req.get("feePerKg"), BigDecimal.ONE),
                    s(req.get("apiEndpoint")),
                    s(req.get("remark")),
                    now,
                    code
            );
        }
        return providerContractRow(code);
    }

    @Transactional
    public Map<String, Object> toggleLgsProviderStatus(String code) {
        Map<String, Object> provider = requiredOne("select status from lgs_provider where provider_code = ?", "provider not found: " + code, code);
        String status = "enabled".equalsIgnoreCase(s(provider.get("status"))) ? "disabled" : "enabled";
        jdbcTemplate.update("update lgs_provider set status = ?, updated_at = ? where provider_code = ?", status, now(), code);
        return providerContractRow(code);
    }

    public Map<String, Object> lgsDeliveries(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from lgs_parcel order by updated_at desc",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "parcelNo", rs.getString("parcel_no"),
                        "orderNo", rs.getString("order_no"),
                        "providerCode", rs.getString("provider_code"),
                        "providerName", rs.getString("provider_name"),
                        "trackingNumber", rs.getString("tracking_number"),
                        "status", rs.getString("status"),
                        "statusText", rs.getString("status_text"),
                        "receiverName", rs.getString("receiver_name"),
                        "receiverPhone", rs.getString("receiver_phone"),
                        "receiverAddress", rs.getString("receiver_address"),
                        "signedBy", rs.getString("signed_by"),
                        "signedAt", format(rs.getTimestamp("signed_at")),
                        "deliveryRemark", rs.getString("delivery_remark"),
                        "createdAt", format(rs.getTimestamp("created_at")),
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        ).stream()
                .filter(row -> equalsValue(row, "providerCode", blankTo(query.get("providerCode"), query.get("carrier"))))
                .filter(row -> equalsValue(row, "status", query.get("status")))
                .filter(row -> containsKeyword(row, blankTo(query.get("waybill"), query.get("keyword")), "trackingNumber", "parcelNo"))
                .filter(row -> containsKeyword(row, query.get("keyword"), "parcelNo", "orderNo", "trackingNumber"))
                .toList();
        Map<String, Object> result = page(rows, query);
        result.put("rows", result.get("list"));
        result.put("carrierOptions", prependAll(providerOptions()));
        result.put("providerOptions", prependAll(providerOptions()));
        result.put("statusOptions", prependAll(simpleOptions("created", "\u5df2\u5efa\u5355", "in_transit", "\u8fd0\u8f93\u4e2d", "signed", "\u5df2\u7b7e\u6536")));
        result.put("watchList", rows.stream().limit(5).map(row -> map(
                "waybill", blankTo(s(row.get("trackingNumber")), s(row.get("parcelNo"))),
                "carrier", s(row.get("providerCode")),
                "status", s(row.get("status"))
        )).toList());
        return result;
    }

    public Map<String, Object> lgsCallbacks() {
        return lgsCallbacks(Map.of());
    }

    public Map<String, Object> lgsCallbacks(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from lgs_callback_record order by created_at desc",
                (rs, rowNum) -> map(
                        "id", rs.getLong("id"),
                        "orderId", rs.getString("order_no"),
                        "providerCode", rs.getString("provider_code"),
                        "callbackType", rs.getString("callback_type"),
                        "status", rs.getString("callback_status"),
                        "requestPayload", rs.getString("request_payload"),
                        "resultPayload", rs.getString("result_payload"),
                        "createdAt", format(rs.getTimestamp("created_at")),
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return map(
                "stats", List.of(
                        map("label", "\u56de\u4f20\u603b\u6570", "value", String.valueOf(rows.size())),
                        map("label", "\u5df2\u63a5\u53d7", "value", String.valueOf(rows.stream().filter(row -> "accepted".equalsIgnoreCase(s(row.get("status")))).count()))
                ),
                "rows", rows,
                "list", rows,
                "pageNo", 1,
                "pageSize", rows.size(),
                "total", rows.size(),
                "actionForm", map(
                        "orderId", "",
                        "providerCode", "sf",
                        "reason", "",
                        "window", "15m"
                ),
                "playbooks", List.of(
                        map("code", "delay_followup", "label", "\u5ef6\u8fdf\u8ddf\u8fdb"),
                        map("code", "intercept_request", "label", "\u62e6\u622a\u7533\u8bf7")
                ),
                "windowOptions", List.of(
                        option("15\u5206\u949f", "15m"),
                        option("30\u5206\u949f", "30m"),
                        option("1\u5c0f\u65f6", "1h")
                )
        );
    }

    @Transactional
    public Map<String, Object> submitLgsIntercept(Map<String, Object> req) {
        String orderId = require(blankTo(s(req.get("orderId")), s(req.get("orderNo"))), "orderId is required");
        String providerCode = blankTo(s(req.get("providerCode")), "sf").toLowerCase();
        Timestamp now = now();
        jdbcTemplate.update(
                "insert into lgs_callback_record (order_no, provider_code, callback_type, callback_status, request_payload, result_payload, created_at, updated_at) values (?, ?, 'intercept', 'accepted', ?, ?, ?, ?)",
                orderId,
                providerCode,
                stringify(req),
                "{\"result\":\"accepted\"}",
                now,
                now
        );
        jdbcTemplate.update(
                "update oms_order set intercept_status = 'intercepted', update_time = ? where order_no = ?",
                now,
                orderId
        );
        return map(
                "orderId", orderId,
                "channel", "LGS 网关",
                "providerCode", providerCode,
                "result", "\u63d0\u4ea4\u6210\u529f",
                "detail", "\u62e6\u622a\u7533\u8bf7\u5df2\u63a5\u53d7"
        );
    }

    public Map<String, Object> iscLedger() {
        return iscLedger(Map.of());
    }

    public Map<String, Object> iscLedger(Map<String, String> query) {
        List<Map<String, Object>> skuRows = jdbcTemplate.query(
                "select warehouse_code, sku_code, sku_name, temp_layer, available_quantity, locked_quantity, total_quantity, updated_at from stock_inventory order by warehouse_code, sku_code",
                (rs, rowNum) -> map(
                        "warehouseCode", rs.getString("warehouse_code"),
                        "sku", rs.getString("sku_code"),
                        "skuCode", rs.getString("sku_code"),
                        "name", rs.getString("sku_name"),
                        "category", categoryFromSku(rs.getString("sku_code"), rs.getString("sku_name")),
                        "temp", tempLabel(rs.getString("temp_layer")),
                        "tempCode", rs.getString("temp_layer"),
                        "warehouse", warehouseName(rs.getString("warehouse_code")),
                        "physical", number(rs.getInt("total_quantity")),
                        "locked", number(rs.getInt("locked_quantity")),
                        "safety", number(safetyStock(rs.getInt("total_quantity"))),
                        "atp", number(Math.max(rs.getInt("available_quantity") - safetyStock(rs.getInt("total_quantity")), 0))
                )
        ).stream()
                .filter(row -> equalsValue(row, "warehouseCode", query.get("warehouse")))
                .filter(row -> equalsValue(row, "tempCode", query.get("temp")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "sku", "name", "warehouse"))
                .toList();
        List<Map<String, Object>> lockRows = jdbcTemplate.query(
                "select o.order_no, i.sku_code, i.quantity, o.update_time from oms_order o join oms_order_item i on o.order_no = i.order_no order by o.update_time desc, i.id desc limit 20",
                (rs, rowNum) -> map(
                        "orderId", rs.getString("order_no"),
                        "sku", rs.getString("sku_code"),
                        "qty", String.valueOf(rs.getInt("quantity")),
                        "time", formatTime(rs.getTimestamp("update_time")),
                        "operator", "OMS 锁库服务"
                )
        );
        int globalAtp = skuRows.stream().mapToInt(row -> parseInt(String.valueOf(row.get("atp")).replace(",", ""), 0)).sum();
        int globalLocked = skuRows.stream().mapToInt(row -> parseInt(String.valueOf(row.get("locked")).replace(",", ""), 0)).sum();
        return map(
                "stats", List.of(
                        map("label", "全局 ATP", "value", number(globalAtp)),
                        map("label", "锁定库存", "value", number(globalLocked)),
                        map("label", "SKU 行数", "value", String.valueOf(skuRows.size()))
                ),
                "skuRows", skuRows,
                "lockRows", lockRows,
                "stats", List.of(
                        map("label", "\u5168\u5c40ATP", "value", number(globalAtp)),
                        map("label", "\u9501\u5b9a\u5e93\u5b58", "value", number(globalLocked)),
                        map("label", "SKU\u884c\u6570", "value", String.valueOf(skuRows.size()))
                ),
                "warehouseOptions", prependAll(warehouseOptions()),
                "tempOptions", prependAll(List.of(option("\u5e38\u6e29", "ambient"), option("\u51b7\u94fe", "cold_chain")))
        );
    }

    public Map<String, Object> iscAdjustment() {
        return iscAdjustment(Map.of());
    }

    public Map<String, Object> iscAdjustment(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select warehouse_code, sku_code, sku_name, available_quantity, locked_quantity, total_quantity, updated_at from stock_inventory order by updated_at desc limit 20",
                (rs, rowNum) -> map(
                        "adjustmentNo", "ADJ-" + rs.getString("warehouse_code") + "-" + rs.getString("sku_code"),
                        "warehouseId", rs.getString("warehouse_code"),
                        "warehouseName", warehouseName(rs.getString("warehouse_code")),
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "availableQuantity", rs.getInt("available_quantity"),
                        "lockedQuantity", rs.getInt("locked_quantity"),
                        "totalQuantity", rs.getInt("total_quantity"),
                        "adjustmentReason", "system_sync",
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return map(
                "form", map(
                        "requestNo", "ADJ-" + System.currentTimeMillis(),
                        "requestType", "manual_adjustment",
                        "warehouse", rows.isEmpty() ? "" : s(rows.get(0).get("warehouseId")),
                        "sku", rows.isEmpty() ? "" : s(rows.get(0).get("skuCode")),
                        "delta", 0,
                        "reason", "cycle_count",
                        "applicant", "system",
                        "status", "draft"
                ),
                "ledgerRows", rows,
                "options", map(
                        "requestTypes", List.of(option("\u4eba\u5de5\u8c03\u6574", "manual_adjustment")),
                        "warehouses", prependAll(warehouseOptions()),
                        "reasons", List.of(option("\u76d8\u70b9", "cycle_count"), option("\u5f02\u5e38\u4fee\u6b63", "exception_fix"))
                ),
                "list", rows,
                "pageNo", 1,
                "pageSize", rows.size(),
                "total", rows.size()
        );
    }

    public Map<String, Object> iscAlerts() {
        return iscAlerts(Map.of());
    }

    public Map<String, Object> iscAlerts(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select warehouse_code, sku_code, sku_name, available_quantity, updated_at from stock_inventory where available_quantity <= 80 order by available_quantity asc, warehouse_code asc",
                (rs, rowNum) -> map(
                        "alertId", "ALT-" + rs.getString("warehouse_code") + "-" + rs.getString("sku_code"),
                        "warehouseId", rs.getString("warehouse_code"),
                        "warehouseName", warehouseName(rs.getString("warehouse_code")),
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "severity", rs.getInt("available_quantity") <= 40 ? "high" : "medium",
                        "availableQuantity", rs.getInt("available_quantity"),
                        "message", "available quantity below threshold",
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return map(
                "stats", List.of(
                        map("label", "\u544a\u8b66SKU", "value", String.valueOf(rows.size())),
                        map("label", "\u9ad8\u98ce\u9669", "value", String.valueOf(rows.stream().filter(row -> "high".equals(row.get("severity"))).count()))
                ),
                "thresholds", rows.stream().map(row -> map(
                        "target", row.get("skuCode"),
                        "scope", row.get("warehouseName"),
                        "min", 80,
                        "current", row.get("availableQuantity"),
                        "gap", Math.max(80 - intOr(row.get("availableQuantity"), 0), 0),
                        "coverageDays", intOr(row.get("availableQuantity"), 0) / 10,
                        "severity", row.get("severity"),
                        "status", intOr(row.get("availableQuantity"), 0) <= 40 ? "\u7d27\u6025" : "\u9884\u8b66",
                        "suggestion", "\u5efa\u8bae\u8865\u8d27\u6216\u8c03\u62e8"
                )).toList(),
                "simulation", map(
                        "replenishmentLeadTime", "\u9ed8\u8ba424\u5c0f\u65f6",
                        "advice", "\u4f18\u5148\u5904\u7406\u9ad8\u98ce\u9669SKU"
                )
        );
    }

    public Map<String, Object> wmsTaskHall() {
        return wmsTaskHall(Map.of());
    }

    public Map<String, Object> wmsTaskHall(Map<String, String> query) {
        List<Map<String, Object>> tasks = jdbcTemplate.query(
                "select * from wms_wave order by created_at desc, id desc",
                (rs, rowNum) -> mapWaveRow(rs)
        ).stream()
                .filter(row -> contains(row, "warehouse", query.get("warehouse")))
                .filter(row -> equalsValue(row, "priority", query.get("priority")))
                .filter(row -> equalsValue(row, "status", query.get("status")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "waveId", "owner", "warehouse", "area"))
                .toList();
        return map(
                "stats", List.of(
                        map("label", "波次数", "value", String.valueOf(tasks.size())),
                        map("label", "执行中", "value", String.valueOf(tasks.stream().filter(item -> "执行中".equals(item.get("status"))).count())),
                        map("label", "待复核", "value", String.valueOf(tasks.stream().filter(item -> "待复核".equals(item.get("status"))).count())),
                        map("label", "订单量", "value", String.valueOf(tasks.stream().mapToInt(item -> intOr(item.get("orders"), 0)).sum()))
                ),
                "tasks", tasks,
                "options", map(
                        "priorities", distinctValues("select distinct priority from wms_wave order by case priority when '高' then 1 when '中' then 2 else 3 end"),
                        "waveStatuses", distinctValues("select distinct status from wms_wave order by id"),
                        "waveTypes", distinctValues("select distinct wave_type from wms_wave order by id"),
                        "warehouses", distinctValues("select distinct warehouse from wms_wave order by warehouse"),
                        "devices", distinctValues("select distinct device from wms_wave where device <> '' order by device")
                ),
                "stats", List.of(
                        map("label", "\u6ce2\u6b21\u603b\u6570", "value", String.valueOf(tasks.size())),
                        map("label", "\u6267\u884c\u4e2d", "value", String.valueOf(tasks.stream().filter(item -> "\u6267\u884c\u4e2d".equals(item.get("status"))).count())),
                        map("label", "\u5f85\u590d\u6838", "value", String.valueOf(tasks.stream().filter(item -> "\u5f85\u590d\u6838".equals(item.get("status"))).count())),
                        map("label", "\u8ba2\u5355\u91cf", "value", String.valueOf(tasks.stream().mapToInt(item -> intOr(item.get("orders"), 0)).sum()))
                ),
                "options", map(
                        "priorities", prependAll(optionize(distinctValues("select distinct priority from wms_wave order by priority"))),
                        "waveStatuses", prependAll(optionize(distinctValues("select distinct status from wms_wave order by id"))),
                        "waveTypes", prependAll(optionize(distinctValues("select distinct wave_type from wms_wave order by id"))),
                        "warehouses", prependAll(optionize(distinctValues("select distinct warehouse from wms_wave order by warehouse"))),
                        "devices", prependAll(optionize(distinctValues("select distinct device from wms_wave where device <> '' order by device")))
                ),
                "defaultForm", map(
                        "warehouse", "",
                        "priority", "",
                        "status", "",
                        "keyword", ""
                )
        );
    }

    public Map<String, Object> wmsWaveDetail(String waveId) {
        return requiredWave(waveId);
    }

    @Transactional
    public Map<String, Object> createWmsWave(Map<String, Object> req) {
        String waveId = require(blankTo(s(req.get("waveId")), "W-" + String.valueOf(System.currentTimeMillis()).substring(8)), "waveId is required");
        Timestamp current = now();
        jdbcTemplate.update(
                "insert into wms_wave (wave_id, warehouse, area, wave_type, orders_count, units_count, priority, status, device, owner, created_at, deadline, remark, source, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                waveId,
                require(s(req.get("warehouse")), "warehouse is required"),
                s(req.get("area")),
                blankTo(s(req.get("waveType")), "订单波次"),
                intOr(req.get("orders"), 0),
                intOr(req.get("units"), 0),
                blankTo(s(req.get("priority")), "中"),
                blankTo(s(req.get("status")), "待领取"),
                s(req.get("device")),
                s(req.get("owner")),
                current,
                timestampOr(req.get("deadline")),
                s(req.get("remark")),
                blankTo(s(req.get("source")), "手工建波"),
                current
        );
        return requiredWave(waveId);
    }

    @Transactional
    public Map<String, Object> updateWmsWave(String waveId, Map<String, Object> req) {
        Map<String, Object> existing = requiredWaveRecord(waveId);
        jdbcTemplate.update(
                "update wms_wave set warehouse = ?, area = ?, wave_type = ?, orders_count = ?, units_count = ?, priority = ?, status = ?, device = ?, owner = ?, deadline = ?, remark = ?, source = ?, updated_at = ? where wave_id = ?",
                blankTo(s(req.get("warehouse")), s(existing.get("warehouse"))),
                blankTo(s(req.get("area")), s(existing.get("area"))),
                blankTo(s(req.get("waveType")), s(existing.get("wave_type"))),
                intOr(req.get("orders"), ((Number) existing.get("orders_count")).intValue()),
                intOr(req.get("units"), ((Number) existing.get("units_count")).intValue()),
                blankTo(s(req.get("priority")), s(existing.get("priority"))),
                blankTo(s(req.get("status")), s(existing.get("status"))),
                blankTo(s(req.get("device")), s(existing.get("device"))),
                blankTo(s(req.get("owner")), s(existing.get("owner"))),
                req.containsKey("deadline") ? timestampOr(req.get("deadline")) : toTimestamp(existing.get("deadline")),
                req.containsKey("remark") ? s(req.get("remark")) : s(existing.get("remark")),
                blankTo(s(req.get("source")), s(existing.get("source"))),
                now(),
                waveId
        );
        return requiredWave(waveId);
    }

    @Transactional
    public Boolean deleteWmsWave(String waveId) {
        return jdbcTemplate.update("delete from wms_wave where wave_id = ?", waveId) > 0;
    }

    public Map<String, Object> wmsPicking() {
        return wmsPicking(Map.of());
    }

    public Map<String, Object> wmsPicking(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from wms_picking_task order by updated_at desc, id desc",
                (rs, rowNum) -> mapPickingRow(rs)
        ).stream()
                .filter(row -> equalsValue(row, "waveId", query.get("waveId")))
                .filter(row -> equalsValue(row, "status", query.get("status")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "sku", "name", "location"))
                .toList();
        return map(
                "stats", List.of(
                        map("label", "任务数", "value", String.valueOf(rows.size())),
                        map("label", "待拣货", "value", String.valueOf(rows.stream().filter(item -> "待拣货".equals(item.get("status"))).count())),
                        map("label", "拣货中", "value", String.valueOf(rows.stream().filter(item -> "拣货中".equals(item.get("status"))).count())),
                        map("label", "异常", "value", String.valueOf(rows.stream().filter(item -> "异常".equals(item.get("status"))).count()))
                ),
                "rows", rows,
                "options", map(
                        "statuses", distinctValues("select distinct status from wms_picking_task order by id"),
                        "waveIds", distinctValues("select distinct wave_id from wms_picking_task order by wave_id"),
                        "operators", distinctValues("select distinct operator from wms_picking_task where operator <> '' order by operator")
                ),
                "stats", List.of(
                        map("label", "\u4efb\u52a1\u603b\u6570", "value", String.valueOf(rows.size())),
                        map("label", "\u5f85\u62e3\u8d27", "value", String.valueOf(rows.stream().filter(item -> "\u5f85\u62e3\u8d27".equals(item.get("status"))).count())),
                        map("label", "\u62e3\u8d27\u4e2d", "value", String.valueOf(rows.stream().filter(item -> "\u62e3\u8d27\u4e2d".equals(item.get("status"))).count())),
                        map("label", "\u5f02\u5e38", "value", String.valueOf(rows.stream().filter(item -> "\u5f02\u5e38".equals(item.get("status"))).count()))
                ),
                "options", map(
                        "statuses", prependAll(optionize(distinctValues("select distinct status from wms_picking_task order by id"))),
                        "waveIds", prependAll(optionize(distinctValues("select distinct wave_id from wms_picking_task order by wave_id")))
                ),
                "defaultForm", map(
                        "waveId", "",
                        "status", "",
                        "keyword", ""
                )
        );
    }

    public Map<String, Object> wmsPickingDetail(Long id) {
        return requiredPicking(id);
    }

    @Transactional
    public Map<String, Object> updateWmsPickingTask(Long id, Map<String, Object> req) {
        Map<String, Object> existing = requiredPickingRecord(id);
        jdbcTemplate.update(
                "update wms_picking_task set location = ?, qty = ?, picked_qty = ?, status = ?, operator = ?, exception_text = ?, remark = ?, updated_at = ? where id = ?",
                blankTo(s(req.get("location")), s(existing.get("location"))),
                intOr(req.get("qty"), ((Number) existing.get("qty")).intValue()),
                intOr(req.get("pickedQty"), ((Number) existing.get("picked_qty")).intValue()),
                blankTo(s(req.get("status")), s(existing.get("status"))),
                blankTo(s(req.get("operator")), s(existing.get("operator"))),
                req.containsKey("exception") ? s(req.get("exception")) : s(existing.get("exception_text")),
                req.containsKey("remark") ? s(req.get("remark")) : s(existing.get("remark")),
                now(),
                id
        );
        return requiredPicking(id);
    }

    public Map<String, Object> wmsPacking() {
        return wmsPacking(Map.of());
    }

    public Map<String, Object> wmsPacking(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from wms_packing_order order by updated_at desc, id desc",
                (rs, rowNum) -> mapPackingRow(rs)
        ).stream()
                .filter(row -> equalsValue(row, "waveId", query.get("waveId")))
                .filter(row -> equalsValue(row, "packageStatus", query.get("status")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "packageNo", "sku", "waybill"))
                .toList();
        return map(
                "stats", List.of(
                        map("label", "包裹数", "value", String.valueOf(rows.size())),
                        map("label", "待打包", "value", String.valueOf(rows.stream().filter(item -> "待打包".equals(item.get("packageStatus"))).count())),
                        map("label", "已打包", "value", String.valueOf(rows.stream().filter(item -> "已打包".equals(item.get("packageStatus"))).count())),
                        map("label", "复核完成", "value", String.valueOf(rows.stream().filter(item -> "复核完成".equals(item.get("packageStatus"))).count()))
                ),
                "rows", rows,
                "options", map(
                        "statuses", distinctValues("select distinct package_status from wms_packing_order order by id"),
                        "waveIds", distinctValues("select distinct wave_id from wms_packing_order order by wave_id"),
                        "printers", distinctValues("select distinct printer from wms_packing_order where printer <> '' order by printer")
                ),
                "stats", List.of(
                        map("label", "\u5305\u88f9\u603b\u6570", "value", String.valueOf(rows.size())),
                        map("label", "\u5f85\u6253\u5305", "value", String.valueOf(rows.stream().filter(item -> "\u5f85\u6253\u5305".equals(item.get("packageStatus"))).count())),
                        map("label", "\u5df2\u6253\u5305", "value", String.valueOf(rows.stream().filter(item -> "\u5df2\u6253\u5305".equals(item.get("packageStatus"))).count())),
                        map("label", "\u590d\u6838\u5b8c\u6210", "value", String.valueOf(rows.stream().filter(item -> "\u590d\u6838\u5b8c\u6210".equals(item.get("packageStatus"))).count()))
                ),
                "options", map(
                        "statuses", prependAll(optionize(distinctValues("select distinct package_status from wms_packing_order order by id"))),
                        "waveIds", prependAll(optionize(distinctValues("select distinct wave_id from wms_packing_order order by wave_id")))
                ),
                "defaultForm", map(
                        "waveId", "",
                        "status", "",
                        "keyword", ""
                )
        );
    }

    public Map<String, Object> wmsPackingDetail(Long id) {
        return requiredPacking(id);
    }

    @Transactional
    public Map<String, Object> updateWmsPackingOrder(Long id, Map<String, Object> req) {
        Map<String, Object> existing = requiredPackingRecord(id);
        jdbcTemplate.update(
                "update wms_packing_order set scanned = ?, required_qty = ?, result = ?, package_status = ?, material = ?, weight = ?, waybill = ?, printer = ?, operator = ?, remark = ?, updated_at = ? where id = ?",
                intOr(req.get("scanned"), ((Number) existing.get("scanned")).intValue()),
                intOr(req.get("required"), ((Number) existing.get("required_qty")).intValue()),
                req.containsKey("result") ? s(req.get("result")) : s(existing.get("result")),
                blankTo(s(req.get("packageStatus")), s(existing.get("package_status"))),
                req.containsKey("material") ? s(req.get("material")) : s(existing.get("material")),
                req.containsKey("weight") ? s(req.get("weight")) : s(existing.get("weight")),
                req.containsKey("waybill") ? s(req.get("waybill")) : s(existing.get("waybill")),
                req.containsKey("printer") ? s(req.get("printer")) : s(existing.get("printer")),
                req.containsKey("operator") ? s(req.get("operator")) : s(existing.get("operator")),
                req.containsKey("remark") ? s(req.get("remark")) : s(existing.get("remark")),
                now(),
                id
        );
        return requiredPacking(id);
    }

    public Map<String, Object> wmsShipment() {
        return wmsShipment(Map.of());
    }

    public Map<String, Object> wmsShipment(Map<String, String> query) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from wms_shipment_record order by updated_at desc, id desc",
                (rs, rowNum) -> mapShipmentRow(rs)
        ).stream()
                .filter(row -> equalsValue(row, "waveId", query.get("waveId")))
                .filter(row -> equalsValue(row, "handover", query.get("status")))
                .filter(row -> equalsValue(row, "carrier", query.get("carrier")))
                .filter(row -> containsKeyword(row, query.get("keyword"), "packageId", "waybill", "dock"))
                .toList();
        return map(
                "stats", List.of(
                        map("label", "包裹数", "value", String.valueOf(rows.size())),
                        map("label", "待装车", "value", String.valueOf(rows.stream().filter(item -> "待装车".equals(item.get("handover"))).count())),
                        map("label", "已装车", "value", String.valueOf(rows.stream().filter(item -> "已装车".equals(item.get("handover"))).count())),
                        map("label", "已交接", "value", String.valueOf(rows.stream().filter(item -> "已交接".equals(item.get("handover"))).count()))
                ),
                "rows", rows,
                "options", map(
                        "statuses", distinctValues("select distinct handover from wms_shipment_record order by id"),
                        "waveIds", distinctValues("select distinct wave_id from wms_shipment_record order by wave_id"),
                        "carriers", distinctValues("select distinct carrier from wms_shipment_record where carrier <> '' order by carrier")
                ),
                "stats", List.of(
                        map("label", "\u5305\u88f9\u603b\u6570", "value", String.valueOf(rows.size())),
                        map("label", "\u5f85\u88c5\u8f66", "value", String.valueOf(rows.stream().filter(item -> "\u5f85\u88c5\u8f66".equals(item.get("handover"))).count())),
                        map("label", "\u5df2\u88c5\u8f66", "value", String.valueOf(rows.stream().filter(item -> "\u5df2\u88c5\u8f66".equals(item.get("handover"))).count())),
                        map("label", "\u5df2\u4ea4\u63a5", "value", String.valueOf(rows.stream().filter(item -> "\u5df2\u4ea4\u63a5".equals(item.get("handover"))).count()))
                ),
                "options", map(
                        "statuses", prependAll(optionize(distinctValues("select distinct handover from wms_shipment_record order by id"))),
                        "waveIds", prependAll(optionize(distinctValues("select distinct wave_id from wms_shipment_record order by wave_id"))),
                        "carriers", prependAll(optionize(distinctValues("select distinct carrier from wms_shipment_record where carrier <> '' order by carrier")))
                ),
                "defaultForm", map(
                        "waveId", "",
                        "status", "",
                        "carrier", "",
                        "keyword", ""
                )
        );
    }

    public Map<String, Object> wmsShipmentDetail(Long id) {
        return requiredShipment(id);
    }

    @Transactional
    public Map<String, Object> updateWmsShipmentRecord(Long id, Map<String, Object> req) {
        Map<String, Object> existing = requiredShipmentRecord(id);
        jdbcTemplate.update(
                "update wms_shipment_record set waybill = ?, carrier = ?, weight = ?, fee = ?, handover = ?, dock = ?, handover_time = ?, operator = ?, remark = ?, updated_at = ? where id = ?",
                req.containsKey("waybill") ? s(req.get("waybill")) : s(existing.get("waybill")),
                req.containsKey("carrier") ? s(req.get("carrier")) : s(existing.get("carrier")),
                req.containsKey("weight") ? s(req.get("weight")) : s(existing.get("weight")),
                req.containsKey("fee") ? s(req.get("fee")) : s(existing.get("fee")),
                blankTo(s(req.get("handover")), s(existing.get("handover"))),
                req.containsKey("dock") ? s(req.get("dock")) : s(existing.get("dock")),
                req.containsKey("handoverTime") ? timestampOr(req.get("handoverTime")) : toTimestamp(existing.get("handover_time")),
                req.containsKey("operator") ? s(req.get("operator")) : s(existing.get("operator")),
                req.containsKey("remark") ? s(req.get("remark")) : s(existing.get("remark")),
                now(),
                id
        );
        return requiredShipment(id);
    }

    public Map<String, Object> dashboardOverview() {
        return map(
                "upstreamPending", count("select count(1) from upstream_order where status = 'pending'"),
                "omsOpen", count("select count(1) from oms_order where status < 60"),
                "inventoryAlerts", count("select count(1) from stock_inventory where available_quantity <= 80"),
                "wmsPending", count("select count(1) from wms_wave where status <> '已完成'"),
                "lgsInTransit", count("select count(1) from lgs_parcel where status = 'in_transit'"),
                "exceptionOpen", count("select count(1) from oms_exception_ticket where status <> 'closed'"),
                "generatedAt", format(now())
        );
    }

    private List<Map<String, Object>> orderRows() {
        return jdbcTemplate.query(
                "select * from oms_order order by create_time desc",
                (rs, rowNum) -> map(
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
                        "totalAmount", money(rs.getBigDecimal("total_amount")),
                        "createTime", format(rs.getTimestamp("create_time")),
                        "dispatchTime", format(rs.getTimestamp("dispatch_time")),
                        "outboundTime", format(rs.getTimestamp("outbound_time")),
                        "updateTime", format(rs.getTimestamp("update_time"))
                )
        );
    }

    private Map<String, Object> currentUserById(long id) {
        Map<String, Object> user = requiredOne("select * from scf_user where id = ?", "user not found: " + id, id);
        return authPayload(user);
    }

    private Map<String, Object> authPayload(Map<String, Object> user) {
        return map(
                "token", "token-" + user.get("user_code"),
                "id", s(user.get("user_code")),
                "name", s(user.get("display_name")),
                "username", s(user.get("username")),
                "role", s(user.get("role_name")),
                "roleCode", s(user.get("role_code")),
                "user", map(
                        "id", s(user.get("user_code")),
                        "username", s(user.get("username")),
                        "name", s(user.get("display_name")),
                        "displayName", s(user.get("display_name")),
                        "roleCode", s(user.get("role_code")),
                        "role", s(user.get("role_name")),
                        "roleName", s(user.get("role_name"))
                )
        );
    }

    private Map<String, Object> navGroup(String id, String label, String title, List<Map<String, Object>> items) {
        return map("id", id, "type", "group", "label", label, "title", title, "items", items);
    }

    private Map<String, Object> navItem(String id, String label, String shortCode) {
        return map("id", id, "label", label, "short", shortCode);
    }

    private Map<String, Object> providerRow(String code) {
        return requiredOne(
                "select provider_code, provider_name, service_scope, contact_name, contact_phone, priority_no, status, sla_hours, base_fee, fee_per_kg, api_endpoint, remark, updated_at from lgs_provider where provider_code = ?",
                "provider not found: " + code,
                code
        );
    }

    private Map<String, Object> providerContractRow(String code) {
        Map<String, Object> row = providerRow(code);
        return map(
                "providerCode", s(row.get("provider_code")),
                "providerName", s(row.get("provider_name")),
                "serviceScope", s(row.get("service_scope")),
                "contactName", s(row.get("contact_name")),
                "contactPhone", s(row.get("contact_phone")),
                "priority", row.get("priority_no"),
                "status", s(row.get("status")),
                "slaHours", row.get("sla_hours"),
                "baseFee", money(decimal(row.get("base_fee"), BigDecimal.ZERO)),
                "feePerKg", money(decimal(row.get("fee_per_kg"), BigDecimal.ZERO)),
                "apiEndpoint", s(row.get("api_endpoint")),
                "remark", s(row.get("remark")),
                "updatedAt", formatDateTime(row.get("updated_at"))
        );
    }

    private void saveRule(Long id, Map<String, Object> req, boolean creating) {
        Timestamp now = now();
        Map<String, Object> existing = creating ? Map.of() : requiredOne("select * from oms_rule where id = ?", "rule not found: " + id, id);
        String warehouse = blankTo(s(req.get("warehouse")), s(req.get("warehouseName")));
        String action = blankTo(s(req.get("action")), s(req.get("actionText")));
        String status = normalizeRuleStatus(blankTo(s(req.get("status")), s(existing.get("status"))));
        if (creating) {
            jdbcTemplate.update(
                    "insert into oms_rule (id, rule_name, rule_type, warehouse_name, priority_no, status, action_text, updated_by, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    id,
                    require(s(req.get("ruleName")), "ruleName is required"),
                    blankTo(s(req.get("ruleType")), "warehouse_route"),
                    require(warehouse, "warehouse is required"),
                    intOr(req.get("priority"), 1),
                    blankTo(status, "enabled"),
                    require(action, "action is required"),
                    blankTo(s(req.get("updatedBy")), "system"),
                    now
            );
        } else {
            jdbcTemplate.update(
                    "update oms_rule set rule_name = ?, rule_type = ?, warehouse_name = ?, priority_no = ?, status = ?, action_text = ?, updated_by = ?, updated_at = ? where id = ?",
                    blankTo(s(req.get("ruleName")), s(existing.get("rule_name"))),
                    blankTo(s(req.get("ruleType")), s(existing.get("rule_type"))),
                    blankTo(warehouse, s(existing.get("warehouse_name"))),
                    intOr(req.get("priority"), ((Number) existing.get("priority_no")).intValue()),
                    blankTo(status, s(existing.get("status"))),
                    blankTo(action, s(existing.get("action_text"))),
                    blankTo(s(req.get("updatedBy")), s(existing.get("updated_by"))),
                    now,
                    id
            );
            if (req.containsKey("conditions")) {
                jdbcTemplate.update("delete from oms_rule_condition where rule_id = ?", id);
            }
        }
        Object conditions = req.get("conditions");
        if (conditions instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> condition) {
                    jdbcTemplate.update(
                            "insert into oms_rule_condition (rule_id, field_code, operator_code, field_value, created_at) values (?, ?, ?, ?, ?)",
                            id,
                            blankTo(s(condition.get("field")), s(condition.get("fieldCode"))),
                            normalizeRuleOperator(blankTo(s(condition.get("operator")), s(condition.get("operatorCode")))),
                            blankTo(s(condition.get("value")), s(condition.get("fieldValue"))),
                            now
                    );
                }
            }
        }
    }

    private Map<String, Object> mapRuleRow(Long id) {
        Map<String, Object> row = requiredOne("select * from oms_rule where id = ?", "rule not found: " + id, id);
        List<Map<String, Object>> conditions = jdbcTemplate.query(
                "select * from oms_rule_condition where rule_id = ? order by id",
                (rs, rowNum) -> map(
                        "field", rs.getString("field_code"),
                        "operator", denormalizeRuleOperator(rs.getString("operator_code")),
                        "value", rs.getString("field_value")
                ),
                id
        );
        return map(
                "id", row.get("id"),
                "ruleName", s(row.get("rule_name")),
                "ruleType", s(row.get("rule_type")),
                "warehouse", s(row.get("warehouse_name")),
                "priority", n((Integer) row.get("priority_no")),
                "status", "enabled".equalsIgnoreCase(s(row.get("status"))) ? "启用" : "停用",
                "action", s(row.get("action_text")),
                "updatedBy", s(row.get("updated_by")),
                "updatedAt", formatDateTime(row.get("updated_at")),
                "conditions", conditions,
                "condition", summarizeConditions(conditions),
                "status", s(row.get("status")),
                "statusText", "enabled".equalsIgnoreCase(s(row.get("status"))) ? "\u542f\u7528" : "\u505c\u7528"
        );
    }

    private String summarizeConditions(List<Map<String, Object>> conditions) {
        return conditions.stream()
                .map(condition -> s(condition.get("field")) + s(condition.get("operator")) + s(condition.get("value")))
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private String normalizeRuleStatus(String status) {
        if ("启用".equals(status)) {
            return "enabled";
        }
        if ("停用".equals(status)) {
            return "disabled";
        }
        return status;
    }

    private String normalizeRuleOperator(String operator) {
        return switch (blankTo(operator, "=")) {
            case "=" -> "eq";
            case "!=" -> "ne";
            default -> operator;
        };
    }

    private String denormalizeRuleOperator(String operator) {
        return switch (blankTo(operator, "eq")) {
            case "eq" -> "=";
            case "ne" -> "!=";
            default -> operator;
        };
    }

    private Map<String, Object> mapExceptionTicketSummary(Map<String, Object> ticket) {
        String severity = s(ticket.get("severity"));
        String status = s(ticket.get("status"));
        String exceptionType = s(ticket.get("exception_type"));
        return map(
                "id", ticket.get("id"),
                "ticketNo", "EX" + ticket.get("id"),
                "orderNo", s(ticket.get("order_no")),
                "reason", s(ticket.get("description")),
                "requestTime", formatDateTime(ticket.get("created_at")),
                "channel", exceptionChannel(exceptionType),
                "currentNode", exceptionCurrentNode(exceptionType, status),
                "status", status,
                "statusText", exceptionStatusText(status),
                "result", exceptionResult(status),
                "level", severityLevelText(severity),
                "levelClass", severity.toLowerCase(),
                "monitorText", blankTo(s(ticket.get("recommended_action")), s(ticket.get("root_cause"))),
                "frozenQty", "ATP " + exceptionFrozenQty(s(ticket.get("order_no"))) + " units"
        );
    }

    private List<Map<String, Object>> buildMonitorSteps(Map<String, Object> ticket) {
        String channel = exceptionChannel(s(ticket.get("exception_type")));
        String status = s(ticket.get("status"));
        return List.of(
                map("key", "oms", "label", "OMS\u5df2\u63a5\u6536\u5f02\u5e38", "state", "done"),
                map("key", "wms", "label", "WMS" + ("WMS".equals(channel) ? "\u5f85\u91ca\u653e" : "\u5df2\u68c0\u67e5"), "state", "failed".equals(status) ? "failed" : "done"),
                map("key", "lgs", "label", "LGS" + ("LGS".equals(channel) ? "\u5df2\u56de\u4f20" : "\u672a\u89e6\u8fbe"), "state", "closed".equals(status) ? "done" : ("failed".equals(status) ? "failed" : "idle"))
        );
    }

    private Map<String, Object> buildExceptionActions(Map<String, Object> ticket, List<Map<String, Object>> actionLogs) {
        boolean released = actionLogs.stream().anyMatch(log -> "release_inventory".equals(log.get("actionCode")));
        boolean rewritten = actionLogs.stream().anyMatch(log -> "rewrite_status".equals(log.get("actionCode")));
        String status = s(ticket.get("status"));
        return map(
                "canReleaseInventory", !"failed".equals(status) && !released,
                "canRewriteOmsStatus", !"failed".equals(status) && !rewritten,
                "canGenerateCompensationLog", true
        );
    }

    private List<Map<String, Object>> buildSimulationCandidates(String province, String city, String tempLayer, String requestedDelivery) {
        return jdbcTemplate.query(
                "select warehouse_code, warehouse_name, city from warehouse where status = 'enabled' order by id",
                (rs, rowNum) -> {
                    String code = rs.getString("warehouse_code");
                    boolean sameCity = city.equalsIgnoreCase(rs.getString("city"));
                    boolean coldChain = !"WH-WH-01".equals(code);
                    boolean sameDay = sameCity;
                    int stockRate = switch (code) {
                        case "WH-WH-01" -> 98;
                        case "WH-CS-01" -> 95;
                        default -> 92;
                    };
                    int capacityRate = switch (code) {
                        case "WH-WH-01" -> 72;
                        case "WH-CS-01" -> 66;
                        default -> 61;
                    };
                    int distanceKm = sameCity ? 18 : ("WH-CS-01".equals(code) ? 356 : 1068);
                    int costIndex = "WH-CS-01".equals(code) ? 78 : ("WH-WH-01".equals(code) ? 86 : 74);
                    int score = Math.max(0,
                            Math.round(stockRate * 35 / 100.0f)
                                    + Math.round((100 - capacityRate) * 25 / 100.0f)
                                    + Math.max(5, 30 - Math.round(distanceKm / 40.0f))
                                    + Math.round(costIndex * 10 / 100.0f)
                                    + (sameCity ? 25 : 0)
                                    - ("Same Day".equalsIgnoreCase(requestedDelivery) && !sameDay ? 20 : 0)
                                    - ("Cold Chain".equalsIgnoreCase(tempLayer) && !coldChain ? 60 : 0)
                    );
                    return map(
                            "code", code,
                            "name", rs.getString("warehouse_name"),
                            "city", rs.getString("city"),
                            "stockRate", stockRate,
                            "capacityRate", capacityRate,
                            "distanceKm", distanceKm,
                            "costIndex", costIndex,
                            "score", score,
                            "dispatchSla", sameDay ? "2h" : "6h",
                            "breakdown", List.of(
                                    map("label", "Stock", "value", Math.round(stockRate * 35 / 100.0f)),
                                    map("label", "Capacity", "value", Math.round((100 - capacityRate) * 25 / 100.0f)),
                                    map("label", "Distance", "value", Math.max(5, 30 - Math.round(distanceKm / 40.0f))),
                                    map("label", "Cost", "value", Math.round(costIndex * 10 / 100.0f))
                            ),
                            "reasons", List.of("Stock rate " + stockRate + "%", "Capacity load " + capacityRate + "%", "Distance " + distanceKm + " km")
                    );
                }
        ).stream().sorted((left, right) -> Integer.compare(intOr(right.get("score"), 0), intOr(left.get("score"), 0))).toList();
    }

    private Map<String, Object> mapWaveRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return map(
                "waveId", rs.getString("wave_id"),
                "warehouse", rs.getString("warehouse"),
                "area", rs.getString("area"),
                "waveType", rs.getString("wave_type"),
                "orders", rs.getInt("orders_count"),
                "units", rs.getInt("units_count"),
                "priority", rs.getString("priority"),
                "status", rs.getString("status"),
                "device", rs.getString("device"),
                "owner", rs.getString("owner"),
                "createdAt", format(rs.getTimestamp("created_at")),
                "deadline", format(rs.getTimestamp("deadline")),
                "remark", rs.getString("remark"),
                "source", rs.getString("source")
        );
    }

    private Map<String, Object> mapPickingRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return map(
                "id", rs.getLong("id"),
                "waveId", rs.getString("wave_id"),
                "location", rs.getString("location"),
                "sku", rs.getString("sku"),
                "name", rs.getString("name"),
                "qty", rs.getInt("qty"),
                "pickedQty", rs.getInt("picked_qty"),
                "status", rs.getString("status"),
                "operator", rs.getString("operator"),
                "updatedAt", format(rs.getTimestamp("updated_at")),
                "exception", rs.getString("exception_text"),
                "remark", rs.getString("remark"),
                "progressText", rs.getInt("picked_qty") + "/" + rs.getInt("qty")
        );
    }

    private Map<String, Object> mapPackingRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return map(
                "id", rs.getLong("id"),
                "packageNo", rs.getString("package_no"),
                "waveId", rs.getString("wave_id"),
                "sku", rs.getString("sku"),
                "scanned", rs.getInt("scanned"),
                "required", rs.getInt("required_qty"),
                "result", rs.getString("result"),
                "packageStatus", rs.getString("package_status"),
                "material", rs.getString("material"),
                "weight", rs.getString("weight"),
                "waybill", rs.getString("waybill"),
                "printer", rs.getString("printer"),
                "operator", rs.getString("operator"),
                "updatedAt", format(rs.getTimestamp("updated_at")),
                "remark", rs.getString("remark"),
                "scannedText", rs.getInt("scanned") + "/" + rs.getInt("required_qty")
        );
    }

    private Map<String, Object> mapShipmentRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return map(
                "id", rs.getLong("id"),
                "packageId", rs.getString("package_id"),
                "waveId", rs.getString("wave_id"),
                "waybill", rs.getString("waybill"),
                "weight", rs.getString("weight"),
                "fee", rs.getString("fee"),
                "handover", rs.getString("handover"),
                "carrier", rs.getString("carrier"),
                "dock", rs.getString("dock"),
                "handoverTime", format(rs.getTimestamp("handover_time")),
                "operator", rs.getString("operator"),
                "updatedAt", format(rs.getTimestamp("updated_at")),
                "remark", rs.getString("remark")
        );
    }

    private Map<String, Object> requiredWave(String waveId) {
        return jdbcTemplate.query("select * from wms_wave where wave_id = ?", (rs, rowNum) -> mapWaveRow(rs), waveId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "wave not found: " + waveId));
    }

    private Map<String, Object> requiredPicking(Long id) {
        return jdbcTemplate.query("select * from wms_picking_task where id = ?", (rs, rowNum) -> mapPickingRow(rs), id).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "picking task not found: " + id));
    }

    private Map<String, Object> requiredPacking(Long id) {
        return jdbcTemplate.query("select * from wms_packing_order where id = ?", (rs, rowNum) -> mapPackingRow(rs), id).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "packing order not found: " + id));
    }

    private Map<String, Object> requiredShipment(Long id) {
        return jdbcTemplate.query("select * from wms_shipment_record where id = ?", (rs, rowNum) -> mapShipmentRow(rs), id).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "shipment record not found: " + id));
    }

    private Map<String, Object> requiredWaveRecord(String waveId) {
        return requiredOne("select * from wms_wave where wave_id = ?", "wave not found: " + waveId, waveId);
    }

    private Map<String, Object> requiredPickingRecord(Long id) {
        return requiredOne("select * from wms_picking_task where id = ?", "picking task not found: " + id, id);
    }

    private Map<String, Object> requiredPackingRecord(Long id) {
        return requiredOne("select * from wms_packing_order where id = ?", "packing order not found: " + id, id);
    }

    private Map<String, Object> requiredShipmentRecord(Long id) {
        return requiredOne("select * from wms_shipment_record where id = ?", "shipment record not found: " + id, id);
    }

    private Map<String, Object> updateSplitMergeStatus(Long id, String status, String resultSummary) {
        Timestamp now = now();
        jdbcTemplate.update(
                "update split_merge_request set status = ?, result_summary = ?, processed_at = ? where id = ?",
                status,
                resultSummary,
                now,
                id
        );
        return requiredOne("select * from split_merge_request where id = ?", "request not found: " + id, id);
    }

    private void updateExceptionStatus(Long id, String status, String result, String actionCode) {
        updateExceptionStatus(id, status, result, actionCode, now(), result);
    }

    private void updateExceptionStatus(Long id, String status, String result, String actionCode, Timestamp actionTime, String remark) {
        jdbcTemplate.update(
                "update oms_exception_ticket set status = ?, updated_at = ? where id = ?",
                status,
                actionTime,
                id
        );
        jdbcTemplate.update(
                "insert into oms_exception_action_log (exception_id, action_code, action_result, remark, operator_name, created_at) values (?, ?, ?, ?, ?, ?)",
                id,
                actionCode,
                result,
                remark,
                "system",
                actionTime
        );
    }

    private List<Map<String, Object>> warehouseOptions() {
        return jdbcTemplate.query(
                "select warehouse_code, warehouse_name from warehouse where status = 'enabled' order by id",
                (rs, rowNum) -> option(rs.getString("warehouse_name"), rs.getString("warehouse_code"))
        );
    }

    private List<Map<String, Object>> providerOptions() {
        return jdbcTemplate.query(
                "select provider_code, provider_name from lgs_provider order by priority_no asc, id asc",
                (rs, rowNum) -> option(rs.getString("provider_name"), rs.getString("provider_code"))
        );
    }

    private List<Map<String, Object>> orderStatusOptions() {
        return List.of(
                option("\u5df2\u521b\u5efa", 10),
                option("\u5df2\u5206\u4ed3", 20),
                option("\u5df2\u4e0b\u53d1", 40),
                option("\u5df2\u51fa\u5e93", 50),
                option("\u5df2\u7b7e\u6536", 60)
        );
    }

    private Map<String, Object> page(List<Map<String, Object>> list, Map<String, String> query) {
        int pageNo = parseInt(query.get("pageNo"), 1);
        int pageSize = parseInt(query.get("pageSize"), Math.max(list.size(), 10));
        int total = list.size();
        int fromIndex = Math.min(Math.max(pageNo - 1, 0) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        return map("list", list.subList(fromIndex, toIndex), "pageNo", pageNo, "pageSize", pageSize, "total", total);
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? null : normalizeJdbcRow(rows.get(0));
    }

    private Map<String, Object> requiredOne(String sql, String message, Object... args) {
        Map<String, Object> row = one(sql, args);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
        return row;
    }

    private boolean exists(String sql, Object arg) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arg);
        return count != null && count > 0;
    }

    private Map<String, Object> normalizeJdbcRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key, normalizeJdbcValue(value)));
        return normalized;
    }

    private Object normalizeJdbcValue(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        }
        return value;
    }

    private long nextId(String table, long fallback) {
        Long id = jdbcTemplate.queryForObject("select coalesce(max(id), ?) + 1 from " + table, Long.class, fallback);
        return id == null ? fallback + 1 : id;
    }

    private String warehouseName(String warehouseCode) {
        if (blank(warehouseCode)) {
            return "";
        }
        Map<String, Object> row = one("select warehouse_name from warehouse where warehouse_code = ?", warehouseCode);
        return row == null ? warehouseCode : s(row.get("warehouse_name"));
    }

    private String providerName(String providerCode) {
        if (blank(providerCode)) {
            return "";
        }
        Map<String, Object> row = one("select provider_name from lgs_provider where provider_code = ?", providerCode);
        return row == null ? providerCode : s(row.get("provider_name"));
    }

    private String exceptionChannel(String exceptionType) {
        return switch (blankTo(exceptionType, "")) {
            case "delivery_delay" -> "LGS";
            case "inventory_shortage" -> "WMS";
            default -> "OMS";
        };
    }

    private String exceptionCurrentNode(String exceptionType, String status) {
        if ("closed".equalsIgnoreCase(status)) {
            return "\u5df2\u8865\u507f";
        }
        return switch (blankTo(exceptionType, "")) {
            case "delivery_delay" -> "\u627f\u8fd0\u5546\u5728\u9014";
            case "inventory_shortage" -> "WMS\u5f85\u91ca\u653e";
            default -> "OMS\u5f85\u5ba1\u6838";
        };
    }

    private String exceptionResult(String status) {
        return "failed".equalsIgnoreCase(status) ? "\u5904\u7406\u5931\u8d25" : "\u5904\u7406\u6210\u529f";
    }

    private int exceptionFrozenQty(String orderNo) {
        Integer qty = jdbcTemplate.queryForObject("select coalesce(sum(quantity), 0) from oms_order_item where order_no = ?", Integer.class, orderNo);
        return qty == null ? 0 : qty;
    }

    private String categoryFromSku(String skuCode, String skuName) {
        String text = (blankTo(skuCode, "") + " " + blankTo(skuName, "")).toLowerCase();
        if (text.contains("yogurt") || text.contains("cold")) {
            return "冷链";
        }
        if (text.contains("milk") || text.contains("drink") || text.contains("gift")) {
            return "饮品";
        }
        return "标品";
    }

    private String tempLabel(String tempLayer) {
        return "cold_chain".equalsIgnoreCase(tempLayer) ? "冷链" : "常温";
    }

    private int safetyStock(int totalQuantity) {
        return Math.max(totalQuantity / 5, 1);
    }

    private String number(int value) {
        return String.format("%,d", value);
    }

    private String formatTime(Timestamp time) {
        if (time == null) {
            return "";
        }
        return time.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private List<String> distinctValues(String sql) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1));
    }

    private List<Map<String, Object>> optionize(List<String> values) {
        return values.stream()
                .filter(value -> !blank(value))
                .map(value -> option(value, value))
                .toList();
    }

    private String channelLabel(String channelCode) {
        return switch (blankTo(channelCode, "")) {
            case "ecommerce" -> "\u7535\u5546";
            case "retail" -> "\u96f6\u552e";
            case "distributor" -> "\u5206\u9500";
            default -> channelCode;
        };
    }

    private Timestamp timestampOr(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return toTimestamp(value);
    }

    private Timestamp toTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        }
        return Timestamp.valueOf(String.valueOf(value).trim().replace("T", " "));
    }

    private String formatDateTime(Object value) {
        return format(toTimestamp(value));
    }

    private Timestamp addMinutes(Timestamp time, int minutes) {
        return Timestamp.valueOf(time.toLocalDateTime().plusMinutes(minutes));
    }

    private String digits(String source, String fallback) {
        String digits = blankTo(source, "").replaceAll("\\D", "");
        return digits.isBlank() ? fallback : digits.substring(Math.max(0, digits.length() - 10));
    }

    private String firstString(List<?> list) {
        return list == null || list.isEmpty() ? "" : String.valueOf(list.get(0));
    }

    private String capitalize(String value) {
        if (blank(value)) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private String severityLevelText(String severity) {
        return switch (blankTo(severity, "").toLowerCase()) {
            case "high" -> "\u9ad8";
            case "medium" -> "\u4e2d";
            case "low" -> "\u4f4e";
            default -> severity;
        };
    }

    private String exceptionStatusText(String status) {
        return switch (blankTo(status, "").toLowerCase()) {
            case "open" -> "\u5f85\u5904\u7406";
            case "processing" -> "\u5904\u7406\u4e2d";
            case "closed" -> "\u5df2\u5173\u95ed";
            case "failed" -> "\u5904\u7406\u5931\u8d25";
            default -> status;
        };
    }

    private String recommendWarehouse(String province, String tempLayer) {
        if ("cold_chain".equalsIgnoreCase(tempLayer) || "Hunan".equalsIgnoreCase(province)) {
            return "WH-CS-01";
        }
        if ("Jiangsu".equalsIgnoreCase(province)) {
            return "WH-NJ-01";
        }
        return "WH-WH-01";
    }

    private String recommendProvider(String tempLayer, String province) {
        if ("cold_chain".equalsIgnoreCase(tempLayer)) {
            return "cold";
        }
        if ("Hunan".equalsIgnoreCase(province)) {
            return "jd";
        }
        return "sf";
    }

    private int estimateHours(String tempLayer, String province) {
        if ("cold_chain".equalsIgnoreCase(tempLayer)) {
            return 36;
        }
        if ("Hunan".equalsIgnoreCase(province)) {
            return 18;
        }
        return 24;
    }

    private boolean isOverdue(Timestamp createTime, int status) {
        if (createTime == null || status >= 60) {
            return false;
        }
        return Duration.between(createTime.toLocalDateTime(), LocalDateTime.now()).toHours() > 48;
    }

    private long agingHours(Timestamp createTime) {
        if (createTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(createTime.toLocalDateTime(), LocalDateTime.now()).toHours(), 0L);
    }

    private int averageAgingHours() {
        List<Timestamp> times = jdbcTemplate.query("select create_time from oms_order", (rs, rowNum) -> rs.getTimestamp(1));
        if (times.isEmpty()) {
            return 0;
        }
        long total = 0L;
        for (Timestamp time : times) {
            total += agingHours(time);
        }
        return (int) (total / times.size());
    }

    private String findExceptionType(String orderNo) {
        Map<String, Object> row = one(
                "select exception_type from oms_exception_ticket where order_no = ? order by updated_at desc limit 1",
                orderNo
        );
        return row == null ? "" : s(row.get("exception_type"));
    }

    private String statusTextFromAction(String actionCode, String fallback) {
        return switch (blankTo(actionCode, "")) {
            case "created" -> "\u5df2\u521b\u5efa";
            case "routed" -> "\u5df2\u5206\u4ed3";
            case "dispatched", "create_task" -> "\u5df2\u4e0b\u53d1";
            case "picked" -> "\u5df2\u62e3\u8d27";
            case "shipped" -> "\u5df2\u51fa\u5e93";
            default -> fallback;
        };
    }

    private List<Map<String, Object>> prependAll(List<Map<String, Object>> options) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(option("\u5168\u90e8", ""));
        result.addAll(options);
        return result;
    }

    private List<Map<String, Object>> simpleOptions(Object... kv) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < kv.length; i += 2) {
            options.add(option(String.valueOf(kv[i + 1]), kv[i]));
        }
        return options;
    }

    private Map<String, Object> option(String label, Object value) {
        return map("label", label, "value", value);
    }

    private boolean contains(Map<String, Object> row, String key, String target) {
        if (blank(target)) {
            return true;
        }
        return s(row.get(key)).toLowerCase().contains(target.trim().toLowerCase());
    }

    private boolean containsKeyword(Map<String, Object> row, String keyword, String... keys) {
        if (blank(keyword)) {
            return true;
        }
        for (String key : keys) {
            if (contains(row, key, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean equalsValue(Map<String, Object> row, String key, String target) {
        if (blank(target)) {
            return true;
        }
        return Objects.equals(s(row.get(key)).toLowerCase(), target.trim().toLowerCase());
    }

    private boolean equalsInt(Map<String, Object> row, String key, String target) {
        if (blank(target)) {
            return true;
        }
        return Objects.equals(String.valueOf(row.get(key)), target.trim());
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(blankTo(value, String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private int intOr(Object value, int defaultValue) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String money(BigDecimal value) {
        return decimal(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal decimal(Object value, BigDecimal defaultValue) {
        try {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value == null || String.valueOf(value).isBlank()) {
                return defaultValue;
            }
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private String stringify(Map<String, Object> req) {
        return req == null ? "{}" : req.toString();
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private String format(Timestamp timestamp) {
        return timestamp == null ? "" : FMT.format(timestamp.toLocalDateTime());
    }

    private String blankTo(String value, String defaultValue) {
        return blank(value) ? defaultValue : value;
    }

    private String require(String value, String message) {
        if (blank(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String s(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            result.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return result;
    }
}
