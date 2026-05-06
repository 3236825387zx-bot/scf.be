package com.scf.lgs.application.service.impl;

import com.scf.common.exception.BusinessException;
import com.scf.lgs.application.enums.LgsErrorCode;
import com.scf.lgs.application.service.LgsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LgsServiceImpl implements LgsService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public LgsServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> estimateRoute(Map<String, Object> request) {
        String province = s(request.get("province"));
        String city = s(request.get("city"));
        BigDecimal weight = decimal(request.get("weight"), BigDecimal.ONE);

        return jdbcTemplate.query(
                "select provider_code, provider_name, service_scope, sla_hours, base_fee, fee_per_kg from lgs_provider where status = 'enabled' order by priority_no asc, id asc",
                (rs, rowNum) -> {
                    int hours = rs.getInt("sla_hours");
                    if ("same_city".equalsIgnoreCase(rs.getString("service_scope")) && !blank(city)) {
                        hours = Math.max(6, hours - 12);
                    }
                    if ("cold_chain".equalsIgnoreCase(s(request.get("tempLayer")))) {
                        hours += 6;
                    }
                    if ("Hunan".equalsIgnoreCase(province)) {
                        hours = Math.max(8, hours - 8);
                    }
                    BigDecimal cost = rs.getBigDecimal("base_fee")
                            .add(rs.getBigDecimal("fee_per_kg").multiply(weight))
                            .setScale(2, RoundingMode.HALF_UP);
                    return map(
                            "providerCode", rs.getString("provider_code"),
                            "providerName", rs.getString("provider_name"),
                            "estimatedCost", cost,
                            "estimatedTime", BigDecimal.valueOf(hours),
                            "warehouseId", recommendWarehouse(province, s(request.get("tempLayer")))
                    );
                }
        );
    }

    @Override
    public Map<String, Object> listProviders(Map<String, String> query) {
        List<Map<String, Object>> filtered = jdbcTemplate.query(
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
                        "baseFee", amount(rs.getBigDecimal("base_fee")),
                        "feePerKg", amount(rs.getBigDecimal("fee_per_kg")),
                        "apiEndpoint", rs.getString("api_endpoint"),
                        "remark", rs.getString("remark"),
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                )
        ).stream()
                .filter(item -> contains(item, "providerCode", query.get("providerCode")))
                .filter(item -> contains(item, "providerName", query.get("providerName")))
                .filter(item -> equalsValue(item, "status", query.get("status")))
                .filter(item -> equalsValue(item, "serviceScope", query.get("serviceScope")))
                .toList();

        Map<String, Object> result = page(filtered, query);
        result.put("statusOptions", prependAll(simpleOptions("enabled", "Enabled", "disabled", "Disabled")));
        result.put("serviceScopeOptions", prependAll(simpleOptions("nationwide", "Nationwide", "same_city", "Same City", "cold_chain", "Cold Chain")));
        return result;
    }

    @Override
    public Map<String, Object> getProvider(String providerCode) {
        Map<String, Object> provider = jdbcTemplate.query(
                "select * from lgs_provider where provider_code = ?",
                rs -> rs.next() ? map(
                        "id", rs.getLong("id"),
                        "providerCode", rs.getString("provider_code"),
                        "providerName", rs.getString("provider_name"),
                        "serviceScope", rs.getString("service_scope"),
                        "contactName", rs.getString("contact_name"),
                        "contactPhone", rs.getString("contact_phone"),
                        "priority", rs.getInt("priority_no"),
                        "status", rs.getString("status"),
                        "slaHours", rs.getInt("sla_hours"),
                        "baseFee", amount(rs.getBigDecimal("base_fee")),
                        "feePerKg", amount(rs.getBigDecimal("fee_per_kg")),
                        "apiEndpoint", rs.getString("api_endpoint"),
                        "remark", rs.getString("remark"),
                        "updatedAt", format(rs.getTimestamp("updated_at"))
                ) : null,
                providerCode
        );
        if (provider == null) {
            throw new BusinessException(LgsErrorCode.PROVIDER_NOT_FOUND, "provider not found: " + providerCode, Map.of("providerCode", providerCode));
        }
        return provider;
    }

    @Override
    @Transactional
    public Map<String, Object> createProvider(Map<String, Object> request) {
        String providerCode = require(s(request.get("providerCode")), LgsErrorCode.PROVIDER_CODE_REQUIRED);
        if (exists("select count(1) from lgs_provider where provider_code = ?", providerCode)) {
            throw new BusinessException(LgsErrorCode.PROVIDER_ALREADY_EXISTS, "provider already exists: " + providerCode, Map.of("providerCode", providerCode));
        }
        Timestamp now = now();
        jdbcTemplate.update(
                "insert into lgs_provider (provider_code, provider_name, service_scope, contact_name, contact_phone, priority_no, status, sla_hours, base_fee, fee_per_kg, api_endpoint, remark, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                providerCode,
                require(s(request.get("providerName")), LgsErrorCode.PROVIDER_NAME_REQUIRED),
                blankTo(s(request.get("serviceScope")), "nationwide"),
                s(request.get("contactName")),
                s(request.get("contactPhone")),
                i(request.get("priority"), 10),
                blankTo(s(request.get("status")), "enabled"),
                i(request.get("slaHours"), 48),
                decimal(request.get("baseFee"), BigDecimal.TEN),
                decimal(request.get("feePerKg"), BigDecimal.ONE),
                s(request.get("apiEndpoint")),
                s(request.get("remark")),
                now,
                now
        );
        return getProvider(providerCode);
    }

    @Override
    @Transactional
    public Map<String, Object> updateProvider(String providerCode, Map<String, Object> request) {
        getProvider(providerCode);
        jdbcTemplate.update(
                "update lgs_provider set provider_name = ?, service_scope = ?, contact_name = ?, contact_phone = ?, priority_no = ?, status = ?, sla_hours = ?, base_fee = ?, fee_per_kg = ?, api_endpoint = ?, remark = ?, updated_at = ? where provider_code = ?",
                require(s(request.get("providerName")), LgsErrorCode.PROVIDER_NAME_REQUIRED),
                blankTo(s(request.get("serviceScope")), "nationwide"),
                s(request.get("contactName")),
                s(request.get("contactPhone")),
                i(request.get("priority"), 10),
                blankTo(s(request.get("status")), "enabled"),
                i(request.get("slaHours"), 48),
                decimal(request.get("baseFee"), BigDecimal.TEN),
                decimal(request.get("feePerKg"), BigDecimal.ONE),
                s(request.get("apiEndpoint")),
                s(request.get("remark")),
                now(),
                providerCode
        );
        return getProvider(providerCode);
    }

    @Override
    @Transactional
    public Map<String, Object> updateProviderStatus(String providerCode, boolean enabled) {
        getProvider(providerCode);
        jdbcTemplate.update(
                "update lgs_provider set status = ?, updated_at = ? where provider_code = ?",
                enabled ? "enabled" : "disabled",
                now(),
                providerCode
        );
        return getProvider(providerCode);
    }

    @Override
    public Map<String, Object> listParcels(Map<String, String> query) {
        List<Map<String, Object>> filtered = jdbcTemplate.query(
                "select * from lgs_parcel order by created_at desc",
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
                .filter(item -> contains(item, "parcelNo", query.get("parcelNo")))
                .filter(item -> contains(item, "orderNo", query.get("orderNo")))
                .filter(item -> equalsValue(item, "providerCode", query.get("providerCode")))
                .filter(item -> equalsValue(item, "status", query.get("status")))
                .filter(item -> containsKeyword(item, query.get("keyword"), "parcelNo", "orderNo", "trackingNumber", "receiverName", "providerName"))
                .toList();

        Map<String, Object> result = page(filtered, query);
        result.put("statusOptions", prependAll(simpleOptions("created", "Created", "in_transit", "In Transit", "signed", "Signed")));
        result.put("providerOptions", prependAll(jdbcTemplate.query(
                "select provider_code, provider_name from lgs_provider order by priority_no asc, id asc",
                (rs, rowNum) -> option(rs.getString("provider_name"), rs.getString("provider_code"))
        )));
        return result;
    }

    @Override
    public Map<String, Object> getParcel(String parcelNo) {
        Map<String, Object> parcel = jdbcTemplate.query(
                "select * from lgs_parcel where parcel_no = ?",
                rs -> rs.next() ? map(
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
                ) : null,
                parcelNo
        );
        if (parcel == null) {
            throw new BusinessException(LgsErrorCode.PARCEL_NOT_FOUND, "parcel not found: " + parcelNo, Map.of("parcelNo", parcelNo));
        }
        parcel.put("tracks", jdbcTemplate.query(
                "select * from lgs_parcel_track where parcel_no = ? order by event_time desc, id desc",
                (rs, rowNum) -> map(
                        "eventCode", rs.getString("event_code"),
                        "eventName", rs.getString("event_name"),
                        "eventTime", format(rs.getTimestamp("event_time")),
                        "location", rs.getString("location"),
                        "operatorName", rs.getString("operator_name"),
                        "remark", rs.getString("remark")
                ),
                parcelNo
        ));
        return parcel;
    }

    @Override
    @Transactional
    public Map<String, Object> deliverParcel(Map<String, Object> request) {
        String orderNo = require(s(request.get("orderNo")), LgsErrorCode.ORDER_NO_REQUIRED);
        String providerCode = blankTo(s(request.get("providerCode")), "sf");
        Map<String, Object> provider = getProvider(providerCode);
        String parcelNo = blankTo(s(request.get("parcelNo")), "LP" + System.currentTimeMillis());
        if (exists("select count(1) from lgs_parcel where parcel_no = ?", parcelNo)) {
            throw new BusinessException(LgsErrorCode.PARCEL_ALREADY_EXISTS, "parcel already exists: " + parcelNo, Map.of("parcelNo", parcelNo));
        }

        Map<String, Object> order = jdbcTemplate.query(
                "select receiver_name, receiver_phone, province, city, district, detail_address from oms_order where order_no = ?",
                rs -> rs.next() ? map(
                        "receiverName", rs.getString("receiver_name"),
                        "receiverPhone", rs.getString("receiver_phone"),
                        "receiverAddress", joinAddress(rs.getString("province"), rs.getString("city"), rs.getString("district"), rs.getString("detail_address"))
                ) : Map.of(),
                orderNo
        );

        Timestamp now = now();
        String trackingNumber = blankTo(s(request.get("trackingNumber")), providerCode.toUpperCase() + System.currentTimeMillis());

        jdbcTemplate.update(
                "insert into lgs_parcel (parcel_no, order_no, provider_code, provider_name, tracking_number, status, status_text, receiver_name, receiver_phone, receiver_address, signed_by, signed_at, delivery_remark, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                parcelNo,
                orderNo,
                providerCode,
                s(provider.get("providerName")),
                trackingNumber,
                "in_transit",
                "In Transit",
                blankTo(s(request.get("receiverName")), s(order.get("receiverName"))),
                blankTo(s(request.get("receiverPhone")), s(order.get("receiverPhone"))),
                blankTo(s(request.get("receiverAddress")), s(order.get("receiverAddress"))),
                "",
                null,
                s(request.get("remark")),
                now,
                now
        );

        appendTrack(parcelNo, "created", "Parcel Created", blankTo(s(request.get("originLocation")), "LGS Gateway"), blankTo(s(request.get("operatorName")), "system"), "parcel delivered to provider", now);
        appendTrack(parcelNo, "in_transit", "In Transit", blankTo(s(request.get("currentLocation")), "Transit Hub"), blankTo(s(request.get("operatorName")), "system"), blankTo(s(request.get("remark")), "awaiting signature"), now);

        jdbcTemplate.update(
                "update oms_order set logistics_provider = ?, logistics_provider_name = ?, tracking_number = ?, status = case when status < 50 then 50 else status end, status_text = case when status < 50 then 'Shipped' else status_text end, update_time = ? where order_no = ?",
                providerCode,
                s(provider.get("providerName")),
                trackingNumber,
                now,
                orderNo
        );

        return getParcel(parcelNo);
    }

    @Override
    @Transactional
    public Map<String, Object> signParcel(String parcelNo, Map<String, Object> request) {
        getParcel(parcelNo);
        Timestamp now = now();
        String signedBy = blankTo(s(request.get("signedBy")), "customer");
        String remark = blankTo(s(request.get("remark")), "parcel signed");
        jdbcTemplate.update(
                "update lgs_parcel set status = 'signed', status_text = 'Signed', signed_by = ?, signed_at = ?, delivery_remark = ?, updated_at = ? where parcel_no = ?",
                signedBy,
                now,
                remark,
                now,
                parcelNo
        );
        appendTrack(parcelNo, "signed", "Signed", blankTo(s(request.get("location")), "Destination"), blankTo(s(request.get("operatorName")), signedBy), remark, now);
        jdbcTemplate.update(
                "update oms_order set status = 60, status_text = 'Delivered', update_time = ? where order_no = (select order_no from lgs_parcel where parcel_no = ?)",
                now,
                parcelNo
        );
        return getParcel(parcelNo);
    }

    @Override
    @Transactional
    public Map<String, Object> appendTracking(String parcelNo, Map<String, Object> request) {
        getParcel(parcelNo);
        appendTrack(
                parcelNo,
                blankTo(s(request.get("eventCode")), "trace"),
                blankTo(s(request.get("eventName")), "Trace Updated"),
                blankTo(s(request.get("location")), "Transit Hub"),
                blankTo(s(request.get("operatorName")), "system"),
                s(request.get("remark")),
                now()
        );
        return getParcel(parcelNo);
    }

    @Override
    public Map<String, Object> dashboard() {
        Integer providerTotal = jdbcTemplate.queryForObject("select count(1) from lgs_provider", Integer.class);
        Integer enabledProviders = jdbcTemplate.queryForObject("select count(1) from lgs_provider where status = 'enabled'", Integer.class);
        Integer parcelTotal = jdbcTemplate.queryForObject("select count(1) from lgs_parcel", Integer.class);
        Integer inTransit = jdbcTemplate.queryForObject("select count(1) from lgs_parcel where status = 'in_transit'", Integer.class);
        Integer signed = jdbcTemplate.queryForObject("select count(1) from lgs_parcel where status = 'signed'", Integer.class);
        return map(
                "providerSummary", map("total", n(providerTotal), "enabled", n(enabledProviders), "disabled", Math.max(0, n(providerTotal) - n(enabledProviders))),
                "parcelSummary", map("total", n(parcelTotal), "inTransit", n(inTransit), "signed", n(signed)),
                "recentParcels", jdbcTemplate.query(
                        "select parcel_no, order_no, provider_name, status_text, updated_at from lgs_parcel order by updated_at desc limit 10",
                        (rs, rowNum) -> map(
                                "parcelNo", rs.getString("parcel_no"),
                                "orderNo", rs.getString("order_no"),
                                "providerName", rs.getString("provider_name"),
                                "statusText", rs.getString("status_text"),
                                "updatedAt", format(rs.getTimestamp("updated_at"))
                        )
                ),
                "generatedAt", format(now())
        );
    }

    private void appendTrack(String parcelNo, String eventCode, String eventName, String location, String operatorName, String remark, Timestamp eventTime) {
        jdbcTemplate.update(
                "insert into lgs_parcel_track (parcel_no, event_code, event_name, event_time, location, operator_name, remark, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                parcelNo,
                eventCode,
                eventName,
                eventTime,
                location,
                operatorName,
                remark,
                eventTime
        );
    }

    private Map<String, Object> page(List<Map<String, Object>> list, Map<String, String> query) {
        int pageNo = i(query.get("pageNo"), 1);
        int pageSize = i(query.get("pageSize"), 10);
        int total = list.size();
        int from = Math.min((pageNo - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return map("list", list.subList(from, to), "pageNo", pageNo, "pageSize", pageSize, "total", total);
    }

    private boolean exists(String sql, Object value) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, value);
        return count != null && count > 0;
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

    private boolean contains(Map<String, Object> item, String key, String value) {
        if (blank(value)) {
            return true;
        }
        return s(item.get(key)).toLowerCase().contains(value.trim().toLowerCase());
    }

    private boolean equalsValue(Map<String, Object> item, String key, String value) {
        if (blank(value)) {
            return true;
        }
        return Objects.equals(s(item.get(key)).toLowerCase(), value.trim().toLowerCase());
    }

    private boolean containsKeyword(Map<String, Object> item, String keyword, String... keys) {
        if (blank(keyword)) {
            return true;
        }
        for (String key : keys) {
            if (contains(item, key, keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> prependAll(List<Map<String, Object>> options) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(option("All", ""));
        list.addAll(options);
        return list;
    }

    private List<Map<String, Object>> simpleOptions(String... kv) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < kv.length; i += 2) {
            list.add(option(kv[i + 1], kv[i]));
        }
        return list;
    }

    private Map<String, Object> option(String label, Object value) {
        return map("label", label, "value", value);
    }

    private String joinAddress(String province, String city, String district, String detailAddress) {
        return String.join(" ", List.of(blankTo(province, ""), blankTo(city, ""), blankTo(district, ""), blankTo(detailAddress, "")).stream().filter(v -> !blank(v)).toList());
    }

    private String require(String value, LgsErrorCode errorCode) {
        if (blank(value)) {
            throw new BusinessException(errorCode);
        }
        return value;
    }

    private String amount(BigDecimal value) {
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

    private int i(Object value, int defaultValue) {
        try {
            int parsed = Integer.parseInt(s(value));
            return parsed > 0 ? parsed : defaultValue;
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
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
