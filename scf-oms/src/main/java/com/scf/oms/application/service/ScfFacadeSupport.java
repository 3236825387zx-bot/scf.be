package com.scf.oms.application.service;

import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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

@Component
class ScfFacadeSupport {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    ScfFacadeSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    JdbcTemplate jdbc() {
        return jdbcTemplate;
    }

    Map<String, Object> page(List<Map<String, Object>> list, Map<String, String> query) {
        int pageNo = parseInt(query.get("pageNo"), 1);
        int pageSize = parseInt(query.get("pageSize"), Math.max(list.size(), 10));
        int total = list.size();
        int fromIndex = Math.min(Math.max(pageNo - 1, 0) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        return map("list", list.subList(fromIndex, toIndex), "pageNo", pageNo, "pageSize", pageSize, "total", total);
    }

    Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? null : normalizeJdbcRow(rows.get(0));
    }

    Map<String, Object> requiredOne(String sql, String message, Object... args) {
        Map<String, Object> row = one(sql, args);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
        return row;
    }

    boolean exists(String sql, Object arg) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arg);
        return count != null && count > 0;
    }

    long nextId(String table, long fallback) {
        Long id = jdbcTemplate.queryForObject("select coalesce(max(id), ?) + 1 from " + table, Long.class, fallback);
        return id == null ? fallback + 1 : id;
    }

    Map<String, Object> normalizeJdbcRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key, normalizeJdbcValue(value)));
        return normalized;
    }

    Object normalizeJdbcValue(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        }
        return value;
    }

    List<Map<String, Object>> warehouseOptions() {
        return jdbcTemplate.query(
                "select warehouse_code, warehouse_name from warehouse where status = 'enabled' order by id",
                (rs, rowNum) -> option(rs.getString("warehouse_name"), rs.getString("warehouse_code"))
        );
    }

    List<Map<String, Object>> providerOptions() {
        return jdbcTemplate.query(
                "select provider_code, provider_name from lgs_provider order by priority_no asc, id asc",
                (rs, rowNum) -> option(rs.getString("provider_name"), rs.getString("provider_code"))
        );
    }

    List<Map<String, Object>> orderStatusOptions() {
        return List.of(
                option("已创建", 10),
                option("已分仓", 20),
                option("已下发", 40),
                option("已出库", 50),
                option("已签收", 60)
        );
    }

    List<Map<String, Object>> prependAll(List<Map<String, Object>> options) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(option("全部", ""));
        result.addAll(options);
        return result;
    }

    List<Map<String, Object>> simpleOptions(Object... kv) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < kv.length; i += 2) {
            options.add(option(String.valueOf(kv[i + 1]), kv[i]));
        }
        return options;
    }

    List<String> distinctValues(String sql) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1));
    }

    List<Map<String, Object>> optionize(List<String> values) {
        return values.stream()
                .filter(value -> !blank(value))
                .map(value -> option(value, value))
                .toList();
    }

    Map<String, Object> option(String label, Object value) {
        return map("label", label, "value", value);
    }

    boolean contains(Map<String, Object> row, String key, String target) {
        if (blank(target)) {
            return true;
        }
        return s(row.get(key)).toLowerCase().contains(target.trim().toLowerCase());
    }

    boolean containsKeyword(Map<String, Object> row, String keyword, String... keys) {
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

    boolean equalsValue(Map<String, Object> row, String key, String target) {
        if (blank(target)) {
            return true;
        }
        return Objects.equals(s(row.get(key)).toLowerCase(), target.trim().toLowerCase());
    }

    boolean equalsInt(Map<String, Object> row, String key, String target) {
        if (blank(target)) {
            return true;
        }
        return Objects.equals(String.valueOf(row.get(key)), target.trim());
    }

    int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(blankTo(value, String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    int intOr(Object value, int defaultValue) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    String money(BigDecimal value) {
        return decimal(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    BigDecimal decimal(Object value, BigDecimal defaultValue) {
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

    int n(Integer value) {
        return value == null ? 0 : value;
    }

    Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    Timestamp timestampOr(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return toTimestamp(value);
    }

    Timestamp toTimestamp(Object value) {
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

    String formatDateTime(Object value) {
        return format(toTimestamp(value));
    }

    Timestamp addMinutes(Timestamp time, int minutes) {
        return Timestamp.valueOf(time.toLocalDateTime().plusMinutes(minutes));
    }

    String digits(String source, String fallback) {
        String digits = blankTo(source, "").replaceAll("\\D", "");
        return digits.isBlank() ? fallback : digits.substring(Math.max(0, digits.length() - 10));
    }

    String firstString(List<?> list) {
        return list == null || list.isEmpty() ? "" : String.valueOf(list.get(0));
    }

    String stringify(Map<String, Object> req) {
        return req == null ? "{}" : req.toString();
    }

    String format(Timestamp timestamp) {
        return timestamp == null ? "" : FMT.format(timestamp.toLocalDateTime());
    }

    String blankTo(String value, String defaultValue) {
        return blank(value) ? defaultValue : value;
    }

    String require(String value, String message) {
        if (blank(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

    boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    String s(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    Map<String, Object> map(Object... kv) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            result.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return result;
    }

    String warehouseName(String warehouseCode) {
        if (blank(warehouseCode)) {
            return "";
        }
        Map<String, Object> row = one("select warehouse_name from warehouse where warehouse_code = ?", warehouseCode);
        return row == null ? warehouseCode : s(row.get("warehouse_name"));
    }

    String providerName(String providerCode) {
        if (blank(providerCode)) {
            return "";
        }
        Map<String, Object> row = one("select provider_name from lgs_provider where provider_code = ?", providerCode);
        return row == null ? providerCode : s(row.get("provider_name"));
    }

    String channelLabel(String channelCode) {
        return switch (blankTo(channelCode, "")) {
            case "ecommerce" -> "电商";
            case "retail" -> "零售";
            case "distributor" -> "分销";
            default -> channelCode;
        };
    }

    String categoryFromSku(String skuCode, String skuName) {
        String text = (blankTo(skuCode, "") + " " + blankTo(skuName, "")).toLowerCase();
        if (text.contains("yogurt") || text.contains("cold")) {
            return "冷链";
        }
        if (text.contains("milk") || text.contains("drink") || text.contains("gift")) {
            return "饮品";
        }
        return "标品";
    }

    String tempLabel(String tempLayer) {
        return "cold_chain".equalsIgnoreCase(tempLayer) ? "冷链" : "常温";
    }

    int safetyStock(int totalQuantity) {
        return Math.max(totalQuantity / 5, 1);
    }

    String number(int value) {
        return String.format("%,d", value);
    }

    String formatTime(Timestamp time) {
        if (time == null) {
            return "";
        }
        return time.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    boolean isOverdue(Timestamp createTime, int status) {
        if (createTime == null || status >= 60) {
            return false;
        }
        return Duration.between(createTime.toLocalDateTime(), LocalDateTime.now()).toHours() > 48;
    }

    long agingHours(Timestamp createTime) {
        if (createTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(createTime.toLocalDateTime(), LocalDateTime.now()).toHours(), 0L);
    }

    int averageAgingHours() {
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

    String findExceptionType(String orderNo) {
        Map<String, Object> row = one(
                "select exception_type from oms_exception_ticket where order_no = ? order by updated_at desc limit 1",
                orderNo
        );
        return row == null ? "" : s(row.get("exception_type"));
    }

    String statusTextFromAction(String actionCode, String fallback) {
        return switch (blankTo(actionCode, "")) {
            case "created" -> "已创建";
            case "routed" -> "已分仓";
            case "dispatched", "create_task" -> "已下发";
            case "picked" -> "已拣货";
            case "shipped" -> "已出库";
            default -> fallback;
        };
    }

    String exceptionChannel(String exceptionType) {
        return switch (blankTo(exceptionType, "")) {
            case "delivery_delay" -> "LGS";
            case "inventory_shortage" -> "WMS";
            default -> "OMS";
        };
    }

    String exceptionCurrentNode(String exceptionType, String status) {
        if ("closed".equalsIgnoreCase(status)) {
            return "已补偿";
        }
        return switch (blankTo(exceptionType, "")) {
            case "delivery_delay" -> "承运商在途";
            case "inventory_shortage" -> "WMS待释放";
            default -> "OMS待审核";
        };
    }

    String exceptionResult(String status) {
        return "failed".equalsIgnoreCase(status) ? "处理失败" : "处理成功";
    }

    int exceptionFrozenQty(String orderNo) {
        Integer qty = jdbcTemplate.queryForObject("select coalesce(sum(quantity), 0) from oms_order_item where order_no = ?", Integer.class, orderNo);
        return qty == null ? 0 : qty;
    }

    String severityLevelText(String severity) {
        return switch (blankTo(severity, "").toLowerCase()) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> severity;
        };
    }

    String exceptionStatusText(String status) {
        return switch (blankTo(status, "").toLowerCase()) {
            case "open" -> "待处理";
            case "processing" -> "处理中";
            case "closed" -> "已关闭";
            case "failed" -> "处理失败";
            default -> status;
        };
    }

    Map<String, Object> mapWaveRow(java.sql.ResultSet rs) throws java.sql.SQLException {
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

    Map<String, Object> mapPickingRow(java.sql.ResultSet rs) throws java.sql.SQLException {
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

    Map<String, Object> mapPackingRow(java.sql.ResultSet rs) throws java.sql.SQLException {
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

    Map<String, Object> mapShipmentRow(java.sql.ResultSet rs) throws java.sql.SQLException {
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

    Map<String, Object> requiredWave(String waveId) {
        return jdbcTemplate.query("select * from wms_wave where wave_id = ?", (rs, rowNum) -> mapWaveRow(rs), waveId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "wave not found: " + waveId));
    }

    Map<String, Object> requiredPicking(Long id) {
        return jdbcTemplate.query("select * from wms_picking_task where id = ?", (rs, rowNum) -> mapPickingRow(rs), id).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "picking task not found: " + id));
    }

    Map<String, Object> requiredPacking(Long id) {
        return jdbcTemplate.query("select * from wms_packing_order where id = ?", (rs, rowNum) -> mapPackingRow(rs), id).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "packing order not found: " + id));
    }

    Map<String, Object> requiredShipment(Long id) {
        return jdbcTemplate.query("select * from wms_shipment_record where id = ?", (rs, rowNum) -> mapShipmentRow(rs), id).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "shipment record not found: " + id));
    }

    Map<String, Object> requiredWaveRecord(String waveId) {
        return requiredOne("select * from wms_wave where wave_id = ?", "wave not found: " + waveId, waveId);
    }

    Map<String, Object> requiredPickingRecord(Long id) {
        return requiredOne("select * from wms_picking_task where id = ?", "picking task not found: " + id, id);
    }

    Map<String, Object> requiredPackingRecord(Long id) {
        return requiredOne("select * from wms_packing_order where id = ?", "packing order not found: " + id, id);
    }

    Map<String, Object> requiredShipmentRecord(Long id) {
        return requiredOne("select * from wms_shipment_record where id = ?", "shipment record not found: " + id, id);
    }

    Map<String, Object> updateSplitMergeStatus(Long id, String status, String resultSummary) {
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

    void updateExceptionStatus(Long id, String status, String result, String actionCode) {
        updateExceptionStatus(id, status, result, actionCode, now(), result);
    }

    void updateExceptionStatus(Long id, String status, String result, String actionCode, Timestamp actionTime, String remark) {
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
}
