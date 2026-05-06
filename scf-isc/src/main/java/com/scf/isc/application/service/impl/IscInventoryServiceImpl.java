package com.scf.isc.application.service.impl;

import com.scf.isc.application.service.IscInventoryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IscInventoryServiceImpl implements IscInventoryService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public IscInventoryServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> queryAtp(Map<String, Object> req) {
        Object skuListObj = req.get("skuList");
        List<?> skuList = skuListObj instanceof List<?> list ? list : List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object skuObj : skuList) {
            String skuCode = String.valueOf(skuObj);
            result.addAll(jdbcTemplate.query(
                    "select warehouse_code, sku_code, available_quantity from stock_inventory where sku_code = ? order by warehouse_code",
                    (rs, rowNum) -> row(
                            "warehouseId", rs.getString("warehouse_code"),
                            "skuCode", rs.getString("sku_code"),
                            "availableQuantity", rs.getInt("available_quantity")
                    ),
                    skuCode
            ));
        }
        return result;
    }

    @Override
    @Transactional
    public Boolean lockStock(Map<String, Object> req) {
        String warehouseId = string(req.get("warehouseId"));
        List<?> skuList = req.get("skuList") instanceof List<?> list ? list : List.of();
        for (Object itemObj : skuList) {
            if (!(itemObj instanceof Map<?, ?> item)) {
                continue;
            }
            String skuCode = string(item.get("skuCode"));
            int quantity = Integer.parseInt(string(item.get("quantity")));
            Integer available = jdbcTemplate.queryForObject(
                    "select available_quantity from stock_inventory where warehouse_code = ? and sku_code = ?",
                    Integer.class,
                    warehouseId,
                    skuCode
            );
            if (available == null || available < quantity) {
                return false;
            }
        }
        for (Object itemObj : skuList) {
            if (!(itemObj instanceof Map<?, ?> item)) {
                continue;
            }
            String skuCode = string(item.get("skuCode"));
            int quantity = Integer.parseInt(string(item.get("quantity")));
            jdbcTemplate.update(
                    "update stock_inventory set available_quantity = available_quantity - ?, locked_quantity = locked_quantity + ? where warehouse_code = ? and sku_code = ?",
                    quantity,
                    quantity,
                    warehouseId,
                    skuCode
            );
        }
        return true;
    }

    @Override
    @Transactional
    public Boolean unlockStock(Map<String, Object> req) {
        String orderId = string(req.get("orderId"));
        String warehouseId = string(req.get("warehouseId"));
        List<Map<String, Object>> rows;
        Object skuListObj = req.get("skuList");
        if (!warehouseId.isBlank() && skuListObj instanceof List<?> skuList && !skuList.isEmpty()) {
            rows = new ArrayList<>();
            for (Object itemObj : skuList) {
                if (!(itemObj instanceof Map<?, ?> item)) {
                    continue;
                }
                rows.add(row(
                        "warehouseCode", warehouseId,
                        "skuCode", string(item.get("skuCode")),
                        "quantity", Integer.parseInt(string(item.get("quantity")))
                ));
            }
        } else {
            rows = jdbcTemplate.query(
                    "select warehouse_code, sku_code, quantity from oms_order_item join oms_order on oms_order_item.order_no = oms_order.order_no where oms_order.order_no = ?",
                    (rs, rowNum) -> row(
                            "warehouseCode", rs.getString("warehouse_code"),
                            "skuCode", rs.getString("sku_code"),
                            "quantity", rs.getInt("quantity")
                    ),
                    orderId
            );
        }
        for (Map<String, Object> item : rows) {
            jdbcTemplate.update(
                    "update stock_inventory set available_quantity = available_quantity + ?, locked_quantity = greatest(locked_quantity - ?, 0) where warehouse_code = ? and sku_code = ?",
                    item.get("quantity"),
                    item.get("quantity"),
                    item.get("warehouseCode"),
                    item.get("skuCode")
            );
        }
        return true;
    }

    @Override
    public Map<String, Object> ledger() {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "select warehouse_code, sku_code, sku_name, temp_layer, available_quantity, locked_quantity, total_quantity, updated_at from stock_inventory order by warehouse_code, sku_code",
                (rs, rowNum) -> row(
                        "warehouseId", rs.getString("warehouse_code"),
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "tempLayer", rs.getString("temp_layer"),
                        "availableQuantity", rs.getInt("available_quantity"),
                        "lockedQuantity", rs.getInt("locked_quantity"),
                        "totalQuantity", rs.getInt("total_quantity"),
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return row("list", list, "pageNo", 1, "pageSize", list.size(), "total", list.size());
    }

    @Override
    public Map<String, Object> adjustment() {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "select warehouse_code, sku_code, sku_name, available_quantity, locked_quantity, total_quantity, updated_at from stock_inventory order by updated_at desc limit 20",
                (rs, rowNum) -> row(
                        "adjustmentNo", "ADJ-" + rs.getString("warehouse_code") + "-" + rs.getString("sku_code"),
                        "warehouseId", rs.getString("warehouse_code"),
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "availableQuantity", rs.getInt("available_quantity"),
                        "lockedQuantity", rs.getInt("locked_quantity"),
                        "totalQuantity", rs.getInt("total_quantity"),
                        "adjustmentReason", "system_sync",
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return row("list", list, "pageNo", 1, "pageSize", list.size(), "total", list.size());
    }

    @Override
    public Map<String, Object> alerts() {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "select warehouse_code, sku_code, sku_name, available_quantity, updated_at from stock_inventory where available_quantity <= 80 order by available_quantity asc, warehouse_code asc",
                (rs, rowNum) -> row(
                        "alertId", "ALT-" + rs.getString("warehouse_code") + "-" + rs.getString("sku_code"),
                        "warehouseId", rs.getString("warehouse_code"),
                        "skuCode", rs.getString("sku_code"),
                        "skuName", rs.getString("sku_name"),
                        "severity", rs.getInt("available_quantity") <= 40 ? "high" : "medium",
                        "availableQuantity", rs.getInt("available_quantity"),
                        "message", "available quantity below threshold",
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        );
        return row("list", list, "pageNo", 1, "pageSize", list.size(), "total", list.size());
    }

    private String format(Timestamp timestamp) {
        return timestamp == null ? "" : FMT.format(timestamp.toLocalDateTime());
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            result.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return result;
    }
}
