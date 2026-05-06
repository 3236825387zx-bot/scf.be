package com.scf.oms.infrastructure.repository;

import com.scf.oms.domain.model.FulfillmentOrder;
import com.scf.oms.domain.model.FulfillmentOrderDetail;
import com.scf.oms.domain.model.ReceiverInfo;
import com.scf.oms.domain.repository.FulfillmentOrderRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Primary
public class JdbcFulfillmentOrderRepository implements FulfillmentOrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFulfillmentOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(FulfillmentOrder order) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from oms_order where order_no = ?",
                Integer.class,
                order.getOrderId()
        );

        if (count != null && count > 0) {
            jdbcTemplate.update(
                    "update oms_order set status = ?, status_text = ?, receiver_name = ?, receiver_phone = ?, province = ?, city = ?, district = ?, detail_address = ?, warehouse_code = ?, warehouse_name = ?, total_amount = ?, update_time = ? where order_no = ?",
                    order.getStatus(),
                    statusText(order.getStatus()),
                    order.getReceiverInfo().getName(),
                    order.getReceiverInfo().getPhone(),
                    order.getReceiverInfo().getProvince(),
                    order.getReceiverInfo().getCity(),
                    order.getReceiverInfo().getDistrict(),
                    order.getReceiverInfo().getDetailAddress(),
                    order.getWarehouseId(),
                    warehouseName(order.getWarehouseId()),
                    order.getTotalAmount(),
                    Timestamp.valueOf(order.getUpdateTime()),
                    order.getOrderId()
            );
            jdbcTemplate.update("delete from oms_order_item where order_no = ?", order.getOrderId());
        } else {
            long newId = parseNumericId(order.getOrderId());
            jdbcTemplate.update(
                    "insert into oms_order (id, order_no, external_no, parent_id, status, status_text, receiver_name, receiver_phone, province, city, district, detail_address, warehouse_code, warehouse_name, logistics_provider, logistics_provider_name, tracking_number, total_amount, route_reason, split_remark, intercept_status, version_no, create_time, dispatch_time, outbound_time, update_time) values (?, ?, ?, null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    newId,
                    order.getOrderId(),
                    order.getExternalOrderId(),
                    order.getStatus(),
                    statusText(order.getStatus()),
                    order.getReceiverInfo().getName(),
                    order.getReceiverInfo().getPhone(),
                    order.getReceiverInfo().getProvince(),
                    order.getReceiverInfo().getCity(),
                    order.getReceiverInfo().getDistrict(),
                    order.getReceiverInfo().getDetailAddress(),
                    order.getWarehouseId(),
                    warehouseName(order.getWarehouseId()),
                    "sf",
                    "SF Express",
                    "",
                    order.getTotalAmount(),
                    "",
                    "",
                    "none",
                    1,
                    Timestamp.valueOf(order.getCreateTime()),
                    order.getStatus() >= 40 ? Timestamp.valueOf(order.getUpdateTime()) : null,
                    null,
                    Timestamp.valueOf(order.getUpdateTime())
            );
        }

        for (FulfillmentOrderDetail detail : order.getDetails()) {
            jdbcTemplate.update(
                    "insert into oms_order_item (order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                    order.getOrderId(),
                    detail.getSkuId(),
                    detail.getSkuName(),
                    "ambient",
                    detail.getQuantity(),
                    detail.getPrice(),
                    detail.getPrice().multiply(java.math.BigDecimal.valueOf(detail.getQuantity())),
                    Timestamp.valueOf(order.getUpdateTime())
            );
        }
    }

    @Override
    public Optional<FulfillmentOrder> findById(String orderId) {
        List<FulfillmentOrder> orders = jdbcTemplate.query(
                "select * from oms_order where order_no = ?",
                (rs, rowNum) -> mapOrder(rs.getString("order_no")),
                orderId
        );
        return orders.stream().findFirst();
    }

    @Override
    public Optional<FulfillmentOrder> findByExternalId(String externalId) {
        List<String> orderNos = jdbcTemplate.query(
                "select order_no from oms_order where external_no = ?",
                (rs, rowNum) -> rs.getString("order_no"),
                externalId
        );
        return orderNos.stream().findFirst().flatMap(this::findById);
    }

    @Override
    public List<FulfillmentOrder> findAll() {
        List<String> orderNos = jdbcTemplate.query(
                "select order_no from oms_order order by create_time desc",
                (rs, rowNum) -> rs.getString("order_no")
        );
        return orderNos.stream().map(this::findById).flatMap(Optional::stream).collect(Collectors.toList());
    }

    private FulfillmentOrder mapOrder(String orderNo) {
        var header = jdbcTemplate.queryForMap("select * from oms_order where order_no = ?", orderNo);
        List<FulfillmentOrderDetail> details = jdbcTemplate.query(
                "select * from oms_order_item where order_no = ? order by id",
                (rs, rowNum) -> new FulfillmentOrderDetail(
                        rs.getString("sku_code"),
                        rs.getString("sku_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price")
                ),
                orderNo
        );

        ReceiverInfo receiverInfo = new ReceiverInfo(
                stringValue(header.get("receiver_name")),
                stringValue(header.get("receiver_phone")),
                stringValue(header.get("province")),
                stringValue(header.get("city")),
                stringValue(header.get("district")),
                stringValue(header.get("detail_address"))
        );

        return FulfillmentOrder.rehydrate(
                stringValue(header.get("order_no")),
                stringValue(header.get("external_no")),
                receiverInfo,
                intValue(header.get("status")),
                stringValue(header.get("warehouse_code")),
                toLocalDateTime(header.get("create_time")),
                toLocalDateTime(header.get("update_time")),
                details
        );
    }

    private String statusText(Integer status) {
        return switch (status) {
            case 10 -> "Created";
            case 20 -> "Routed";
            case 30 -> "Locked";
            case 40 -> "Dispatched";
            case 50 -> "Shipped";
            case 60 -> "Delivered";
            case 90 -> "Cancelled";
            default -> "Unknown";
        };
    }

    private String warehouseName(String warehouseCode) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            return null;
        }
        List<String> names = jdbcTemplate.query(
                "select warehouse_name from warehouse where warehouse_code = ?",
                (rs, rowNum) -> rs.getString(1),
                warehouseCode
        );
        return names.isEmpty() ? warehouseCode : names.get(0);
    }

    private long parseNumericId(String orderNo) {
        return Long.parseLong(orderNo.replace("FO", ""));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Integer intValue(Object value) {
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value).replace(" ", "T"));
    }
}
