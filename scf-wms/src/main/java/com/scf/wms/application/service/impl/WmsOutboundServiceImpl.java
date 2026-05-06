package com.scf.wms.application.service.impl;

import com.scf.common.exception.BusinessException;
import com.scf.wms.application.enums.WmsErrorCode;
import com.scf.wms.application.service.WmsOutboundService;
import com.scf.wms.interfaces.dto.OutboundCreateRequest;
import com.scf.wms.interfaces.dto.TaskActionRequest;
import com.scf.wms.interfaces.dto.TaskShipRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WmsOutboundServiceImpl implements WmsOutboundService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public WmsOutboundServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public boolean createOutboundTask(OutboundCreateRequest request) {
        validateCreateRequest(request);

        Map<String, Object> order = queryForNullableMap(
                "select * from oms_order where order_no = ?",
                request.getOrderId()
        );
        if (order == null) {
            throw new BusinessException(WmsErrorCode.ORDER_NOT_FOUND, "oms order not found: " + request.getOrderId(), request.getOrderId());
        }

        Map<String, Object> warehouse = queryForNullableMap(
                "select warehouse_code, warehouse_name from warehouse where warehouse_code = ?",
                request.getWarehouseId()
        );
        if (warehouse == null) {
            throw new BusinessException(WmsErrorCode.WAREHOUSE_NOT_FOUND, "warehouse not found: " + request.getWarehouseId(), request.getWarehouseId());
        }

        List<String> existingTaskNos = jdbcTemplate.query(
                "select task_no from wms_outbound_task where order_no = ? order by id desc",
                (rs, rowNum) -> rs.getString(1),
                request.getOrderId()
        );
        if (!existingTaskNos.isEmpty()) {
            return true;
        }

        String taskNo = "WT" + System.currentTimeMillis();
        Timestamp now = now();
        int totalQty = request.getItems().stream().mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();

        jdbcTemplate.update(
                "insert into wms_outbound_task (task_no, order_no, warehouse_code, warehouse_name, receiver_name, receiver_phone, receiver_address, status, status_text, total_qty, total_sku_count, logistics_provider, logistics_provider_name, tracking_number, created_at, picked_at, shipped_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                taskNo,
                request.getOrderId(),
                request.getWarehouseId(),
                stringValue(warehouse.get("warehouse_name")),
                request.getReceiverInfo().getName(),
                request.getReceiverInfo().getPhone(),
                request.getReceiverInfo().getAddress(),
                "created",
                "Created",
                totalQty,
                request.getItems().size(),
                "",
                "",
                "",
                now,
                null,
                null,
                now
        );

        for (OutboundCreateRequest.OutboundItem item : request.getItems()) {
            Map<String, Object> omsItem = queryForNullableMap(
                    "select sku_name, unit_price from oms_order_item where order_no = ? and sku_code = ?",
                    request.getOrderId(),
                    item.getSkuCode()
            );
            String skuName = omsItem == null ? item.getSkuCode() : stringValue(omsItem.get("sku_name"));
            Object unitPrice = omsItem == null ? 0 : omsItem.get("unit_price");
            jdbcTemplate.update(
                    "insert into wms_outbound_task_item (task_no, order_no, sku_code, sku_name, quantity, picked_quantity, shipped_quantity, unit_price, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    taskNo,
                    request.getOrderId(),
                    item.getSkuCode(),
                    skuName,
                    item.getQuantity(),
                    0,
                    0,
                    unitPrice,
                    now,
                    now
            );
        }

        jdbcTemplate.update(
                "update oms_order set status = 40, status_text = 'Dispatched', warehouse_code = ?, warehouse_name = ?, dispatch_time = coalesce(dispatch_time, ?), update_time = ? where order_no = ?",
                request.getWarehouseId(),
                stringValue(warehouse.get("warehouse_name")),
                now,
                now,
                request.getOrderId()
        );
        insertTaskLog(taskNo, now, "create", "system", "outbound task created");
        insertOmsLog(request.getOrderId(), now, "WMS", "create_task", "system", "wms outbound task created: " + taskNo);
        return true;
    }

    @Override
    public Map<String, Object> listTasks(Map<String, String> query) {
        String taskNo = "%" + blankTo(query.get("taskNo"), "") + "%";
        String orderNo = "%" + blankTo(query.get("orderNo"), "") + "%";
        String warehouseId = "%" + blankTo(query.get("warehouseId"), "") + "%";
        String status = "%" + blankTo(query.get("status"), "") + "%";
        List<Map<String, Object>> list = jdbcTemplate.query(
                "select * from wms_outbound_task where task_no like ? and order_no like ? and warehouse_code like ? and status like ? order by created_at desc",
                (rs, rowNum) -> map(
                        "taskNo", rs.getString("task_no"),
                        "orderNo", rs.getString("order_no"),
                        "warehouseId", rs.getString("warehouse_code"),
                        "warehouseName", rs.getString("warehouse_name"),
                        "receiverName", rs.getString("receiver_name"),
                        "status", rs.getString("status"),
                        "statusText", rs.getString("status_text"),
                        "totalQty", rs.getInt("total_qty"),
                        "totalSkuCount", rs.getInt("total_sku_count"),
                        "trackingNumber", rs.getString("tracking_number"),
                        "createdAt", format(rs.getTimestamp("created_at")),
                        "pickedAt", format(rs.getTimestamp("picked_at")),
                        "shippedAt", format(rs.getTimestamp("shipped_at"))
                ),
                taskNo,
                orderNo,
                warehouseId,
                status
        );
        return map("list", list, "pageNo", 1, "pageSize", list.size(), "total", list.size());
    }

    @Override
    public Map<String, Object> getTask(String taskNo) {
        Map<String, Object> header = queryTaskHeader(taskNo);
        if (header == null) {
            throw new BusinessException(WmsErrorCode.OUTBOUND_TASK_NOT_FOUND, "outbound task not found: " + taskNo, taskNo);
        }
        return buildTaskDetail(header);
    }

    @Override
    public Map<String, Object> getTaskByOrderNo(String orderNo) {
        List<String> taskNos = jdbcTemplate.query(
                "select task_no from wms_outbound_task where order_no = ? order by id desc",
                (rs, rowNum) -> rs.getString(1),
                orderNo
        );
        if (taskNos.isEmpty()) {
            throw new BusinessException(WmsErrorCode.OUTBOUND_TASK_NOT_FOUND, "outbound task not found for order: " + orderNo, orderNo);
        }
        return getTask(taskNos.get(0));
    }

    @Override
    @Transactional
    public Map<String, Object> pickTask(String taskNo, TaskActionRequest request) {
        Map<String, Object> header = requiredTask(taskNo);
        ensureStatus(header, "created");
        Timestamp now = now();
        String operator = request == null || isBlank(request.getOperator()) ? "system" : request.getOperator();
        String remark = request == null || isBlank(request.getRemark()) ? "task picked" : request.getRemark();

        jdbcTemplate.update(
                "update wms_outbound_task set status = 'picked', status_text = 'Picked', picked_at = ?, updated_at = ? where task_no = ?",
                now,
                now,
                taskNo
        );
        jdbcTemplate.update(
                "update wms_outbound_task_item set picked_quantity = quantity, updated_at = ? where task_no = ?",
                now,
                taskNo
        );
        insertTaskLog(taskNo, now, "pick", operator, remark);
        insertOmsLog(stringValue(header.get("order_no")), now, "WMS", "picked", operator, remark);
        return getTask(taskNo);
    }

    @Override
    @Transactional
    public Map<String, Object> shipTask(String taskNo, TaskShipRequest request) {
        if (request == null) {
            throw new BusinessException(WmsErrorCode.INVALID_REQUEST, "ship request is required");
        }
        Map<String, Object> header = requiredTask(taskNo);
        ensureStatus(header, "picked");
        Timestamp now = now();
        String orderNo = stringValue(header.get("order_no"));
        String provider = blankTo(request.getLogisticsProvider(), "sf");
        String providerName = blankTo(request.getLogisticsProviderName(), "SF Express");
        String trackingNumber = blankTo(request.getTrackingNumber(), "TRK" + System.currentTimeMillis());
        String operator = blankTo(request.getOperator(), "system");
        String remark = blankTo(request.getRemark(), "task shipped");

        jdbcTemplate.update(
                "update wms_outbound_task set status = 'shipped', status_text = 'Shipped', logistics_provider = ?, logistics_provider_name = ?, tracking_number = ?, shipped_at = ?, updated_at = ? where task_no = ?",
                provider,
                providerName,
                trackingNumber,
                now,
                now,
                taskNo
        );
        jdbcTemplate.update(
                "update wms_outbound_task_item set shipped_quantity = quantity, updated_at = ? where task_no = ?",
                now,
                taskNo
        );
        jdbcTemplate.update(
                "update oms_order set status = 50, status_text = 'Shipped', logistics_provider = ?, logistics_provider_name = ?, tracking_number = ?, outbound_time = ?, update_time = ? where order_no = ?",
                provider,
                providerName,
                trackingNumber,
                now,
                now,
                orderNo
        );
        insertTaskLog(taskNo, now, "ship", operator, remark);
        insertOmsLog(orderNo, now, "WMS", "shipped", operator, remark);
        return getTask(taskNo);
    }

    @Override
    public Map<String, Object> dashboard() {
        Integer total = jdbcTemplate.queryForObject("select count(1) from wms_outbound_task", Integer.class);
        Integer created = jdbcTemplate.queryForObject("select count(1) from wms_outbound_task where status = 'created'", Integer.class);
        Integer picked = jdbcTemplate.queryForObject("select count(1) from wms_outbound_task where status = 'picked'", Integer.class);
        Integer shipped = jdbcTemplate.queryForObject("select count(1) from wms_outbound_task where status = 'shipped'", Integer.class);
        List<Map<String, Object>> recent = jdbcTemplate.query(
                "select * from wms_outbound_task order by updated_at desc limit 10",
                (rs, rowNum) -> map(
                        "taskNo", rs.getString("task_no"),
                        "orderNo", rs.getString("order_no"),
                        "status", rs.getString("status"),
                        "statusText", rs.getString("status_text"),
                        "warehouseName", rs.getString("warehouse_name"),
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return map(
                "cards", List.of(
                        map("label", "Total Tasks", "value", value(total)),
                        map("label", "Created", "value", value(created)),
                        map("label", "Picked", "value", value(picked)),
                        map("label", "Shipped", "value", value(shipped))
                ),
                "recentTasks", recent
        );
    }

    private void validateCreateRequest(OutboundCreateRequest request) {
        if (request == null) {
            throw new BusinessException(WmsErrorCode.INVALID_REQUEST, "request body is required");
        }
        if (isBlank(request.getOrderId())) {
            throw new BusinessException(WmsErrorCode.INVALID_REQUEST, "orderId is required");
        }
        if (isBlank(request.getWarehouseId())) {
            throw new BusinessException(WmsErrorCode.INVALID_REQUEST, "warehouseId is required");
        }
        if (request.getReceiverInfo() == null || isBlank(request.getReceiverInfo().getName()) || isBlank(request.getReceiverInfo().getAddress())) {
            throw new BusinessException(WmsErrorCode.INVALID_REQUEST, "receiverInfo is incomplete");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(WmsErrorCode.INVALID_REQUEST, "items must not be empty");
        }
        for (OutboundCreateRequest.OutboundItem item : request.getItems()) {
            if (item == null || isBlank(item.getSkuCode()) || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(WmsErrorCode.INVALID_REQUEST, "items contain invalid sku or quantity");
            }
        }
    }

    private Map<String, Object> requiredTask(String taskNo) {
        Map<String, Object> header = queryTaskHeader(taskNo);
        if (header == null) {
            throw new BusinessException(WmsErrorCode.OUTBOUND_TASK_NOT_FOUND, "outbound task not found: " + taskNo, taskNo);
        }
        return header;
    }

    private void ensureStatus(Map<String, Object> header, String expectedStatus) {
        String actual = stringValue(header.get("status"));
        if (!expectedStatus.equals(actual)) {
            throw new BusinessException(
                    WmsErrorCode.INVALID_TASK_STATUS,
                    "task status must be " + expectedStatus + " but was " + actual,
                    map("taskNo", header.get("task_no"), "status", actual)
            );
        }
    }

    private Map<String, Object> queryTaskHeader(String taskNo) {
        return queryForNullableMap("select * from wms_outbound_task where task_no = ?", taskNo);
    }

    private Map<String, Object> buildTaskDetail(Map<String, Object> header) {
        String taskNo = stringValue(header.get("task_no"));
        List<Map<String, Object>> items = jdbcTemplate.query(
                "select * from wms_outbound_task_item where task_no = ? order by id",
                (rs, rowNum) -> map(
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "quantity", rs.getInt("quantity"),
                        "pickedQuantity", rs.getInt("picked_quantity"),
                        "shippedQuantity", rs.getInt("shipped_quantity"),
                        "unitPrice", rs.getBigDecimal("unit_price")
                ),
                taskNo
        );
        List<Map<String, Object>> logs = jdbcTemplate.query(
                "select * from wms_outbound_task_log where task_no = ? order by log_time desc, id desc",
                (rs, rowNum) -> map(
                        "time", format(rs.getTimestamp("log_time")),
                        "action", rs.getString("action_code"),
                        "operator", rs.getString("operator_name"),
                        "remark", rs.getString("remark")
                ),
                taskNo
        );
        return map(
                "base", map(
                        "taskNo", taskNo,
                        "orderNo", stringValue(header.get("order_no")),
                        "warehouseId", stringValue(header.get("warehouse_code")),
                        "warehouseName", stringValue(header.get("warehouse_name")),
                        "receiverName", stringValue(header.get("receiver_name")),
                        "receiverPhone", stringValue(header.get("receiver_phone")),
                        "receiverAddress", stringValue(header.get("receiver_address")),
                        "status", stringValue(header.get("status")),
                        "statusText", stringValue(header.get("status_text")),
                        "totalQty", header.get("total_qty"),
                        "totalSkuCount", header.get("total_sku_count"),
                        "logisticsProvider", stringValue(header.get("logistics_provider")),
                        "logisticsProviderName", stringValue(header.get("logistics_provider_name")),
                        "trackingNumber", stringValue(header.get("tracking_number")),
                        "createdAt", format((Timestamp) header.get("created_at")),
                        "pickedAt", format((Timestamp) header.get("picked_at")),
                        "shippedAt", format((Timestamp) header.get("shipped_at")),
                        "updatedAt", format((Timestamp) header.get("updated_at"))
                ),
                "items", items,
                "logs", logs
        );
    }

    private void insertTaskLog(String taskNo, Timestamp time, String action, String operator, String remark) {
        jdbcTemplate.update(
                "insert into wms_outbound_task_log (task_no, log_time, action_code, operator_name, remark, created_at) values (?, ?, ?, ?, ?, ?)",
                taskNo,
                time,
                action,
                operator,
                remark,
                time
        );
    }

    private void insertOmsLog(String orderNo, Timestamp time, String node, String action, String operator, String remark) {
        jdbcTemplate.update(
                "insert into oms_order_log (order_no, log_time, node_code, action_code, operator_name, remark, created_at) values (?, ?, ?, ?, ?, ?, ?)",
                orderNo,
                time,
                node,
                action,
                operator,
                remark,
                time
        );
    }

    private Map<String, Object> queryForNullableMap(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private String format(Timestamp timestamp) {
        return timestamp == null ? "" : FMT.format(timestamp.toLocalDateTime());
    }

    private String blankTo(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return out;
    }
}
