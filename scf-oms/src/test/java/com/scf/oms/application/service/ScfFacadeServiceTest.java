package com.scf.oms.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ScfFacadeServiceTest {

    private JdbcTemplate jdbcTemplate;
    private FulfillmentOrderService fulfillmentOrderService;
    private ScfFacadeService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        fulfillmentOrderService = Mockito.mock(FulfillmentOrderService.class);
        service = new ScfFacadeService(jdbcTemplate, fulfillmentOrderService);
    }

    @Test
    void omsOrdersSupportsFilteringAndPagination() {
        when(jdbcTemplate.query(Mockito.eq("select * from oms_order order by create_time desc"), Mockito.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(
                        Map.of("id", 1L, "orderNo", "FO1001", "externalNo", "EXT-ALPHA", "status", 40, "warehouseId", "WH-WH-01", "receiverName", "Alice", "trackingNumber", "", "interceptStatus", "none"),
                        Map.of("id", 2L, "orderNo", "FO1002", "externalNo", "EXT-BETA", "status", 20, "warehouseId", "WH-NJ-01", "receiverName", "Bob", "trackingNumber", "", "interceptStatus", "none"),
                        Map.of("id", 3L, "orderNo", "FO1003", "externalNo", "EXT-GAMMA", "status", 40, "warehouseId", "WH-WH-01", "receiverName", "Cindy", "trackingNumber", "", "interceptStatus", "intercepted")
                ));
        when(jdbcTemplate.query(Mockito.eq("select warehouse_code, warehouse_name from warehouse where status = 'enabled' order by id"), Mockito.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(Map.of("label", "Wuhan DC", "value", "WH-WH-01")));
        when(jdbcTemplate.query(Mockito.eq("select provider_code, provider_name from lgs_provider order by priority_no asc, id asc"), Mockito.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(Map.of("label", "SF Express", "value", "sf")));

        Map<String, Object> result = service.omsOrders(Map.of(
                "status", "40",
                "warehouseId", "WH-WH-01",
                "pageNo", "1",
                "pageSize", "1"
        ));

        assertThat(result.get("total")).isEqualTo(2);
        assertThat((List<?>) result.get("list")).hasSize(1);
        assertThat((List<?>) result.get("statusOptions")).isNotEmpty();
    }

    @Test
    void dashboardIncludesStatusSummary() {
        when(jdbcTemplate.queryForObject("select count(1) from oms_order where status = 10", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForObject("select count(1) from oms_order where status = 20", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForObject("select count(1) from oms_order where status = 40", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForObject("select count(1) from oms_order where status = 50", Integer.class)).thenReturn(0);
        when(jdbcTemplate.queryForObject("select count(1) from oms_order where status = 60", Integer.class)).thenReturn(0);
        when(jdbcTemplate.queryForObject("select count(1) from split_merge_request where status = 'pending'", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForObject("select count(1) from oms_order where status >= 20", Integer.class)).thenReturn(2);
        when(jdbcTemplate.queryForObject("select count(1) from oms_order where parent_id is not null", Integer.class)).thenReturn(0);
        when(jdbcTemplate.queryForObject("select count(1) from oms_exception_ticket where status <> 'closed'", Integer.class)).thenReturn(1);
        when(jdbcTemplate.query(Mockito.eq("select * from oms_order order by update_time desc limit 10"), Mockito.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(Map.of("id", 1L, "orderNo", "FO1001", "status", 40, "updateTime", "2026-03-27 10:00:00")));
        when(jdbcTemplate.query(Mockito.eq("select warehouse_code, warehouse_name from warehouse where status = 'enabled' order by id"), Mockito.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(Map.of("label", "Wuhan DC", "value", "WH-WH-01")));
        when(jdbcTemplate.query(Mockito.eq("select provider_code, provider_name from lgs_provider order by priority_no asc, id asc"), Mockito.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(Map.of("label", "SF Express", "value", "sf")));
        when(jdbcTemplate.query(Mockito.eq("select create_time from oms_order"), Mockito.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(java.sql.Timestamp.valueOf("2026-03-27 10:00:00")));
        when(jdbcTemplate.queryForList(Mockito.eq("select exception_type from oms_exception_ticket where order_no = ? order by updated_at desc limit 1"), Mockito.<Object[]>any()))
                .thenReturn(List.of());

        Map<String, Object> result = service.omsDashboard();

        assertThat(result.get("pendingSplitRequests")).isEqualTo(1);
        assertThat((List<?>) result.get("orders")).hasSize(1);
        assertThat((List<?>) result.get("orderStatusSummary")).hasSize(5);
    }

    @Test
    void omsOrderDetailSupportsLocalDateTimeValuesFromQueryForList() {
        Map<String, Object> baseRow = new LinkedHashMap<>();
        baseRow.put("id", 1L);
        baseRow.put("order_no", "FO1001");
        baseRow.put("external_no", "EXT1001");
        baseRow.put("parent_id", null);
        baseRow.put("status", 40);
        baseRow.put("status_text", "Dispatched");
        baseRow.put("receiver_name", "Alice");
        baseRow.put("receiver_phone", "13800000000");
        baseRow.put("province", "Hubei");
        baseRow.put("city", "Wuhan");
        baseRow.put("district", "Hongshan");
        baseRow.put("detail_address", "Optics Valley Road 188");
        baseRow.put("warehouse_code", "WH-WH-01");
        baseRow.put("warehouse_name", "Wuhan DC");
        baseRow.put("logistics_provider", "sf");
        baseRow.put("logistics_provider_name", "SF Express");
        baseRow.put("tracking_number", "SF123");
        baseRow.put("total_amount", BigDecimal.TEN);
        baseRow.put("route_reason", "nearest");
        baseRow.put("split_remark", "");
        baseRow.put("intercept_status", "none");
        baseRow.put("version_no", 1);
        baseRow.put("create_time", LocalDateTime.of(2026, 4, 6, 10, 0));
        baseRow.put("dispatch_time", LocalDateTime.of(2026, 4, 6, 10, 10));
        baseRow.put("outbound_time", LocalDateTime.of(2026, 4, 6, 10, 20));
        baseRow.put("update_time", LocalDateTime.of(2026, 4, 6, 10, 30));

        when(jdbcTemplate.queryForList("select * from oms_order where id = ?", 1L))
                .thenReturn(List.of(baseRow));
        when(jdbcTemplate.query(Mockito.eq("select * from oms_order_item where order_no = ? order by id"), Mockito.any(org.springframework.jdbc.core.RowMapper.class), Mockito.eq("FO1001")))
                .thenReturn(List.of(Map.of("id", 11L, "skuId", "SKU1", "skuCode", "SKU1", "skuName", "Gift Box", "tempLayer", "ambient", "quantity", 1, "splitAmount", 1, "unitPrice", "10.00", "amount", "10.00", "weight", "1.00")));
        when(jdbcTemplate.query(Mockito.eq("select * from oms_order_log where order_no = ? order by log_time desc, id desc"), Mockito.any(org.springframework.jdbc.core.RowMapper.class), Mockito.eq("FO1001")))
                .thenReturn(List.of());

        Map<String, Object> detail = service.omsOrderDetail(1L);

        Map<?, ?> base = (Map<?, ?>) detail.get("base");
        assertThat(base.get("createTime")).isEqualTo("2026-04-06 10:00:00");
        assertThat(base.get("dispatchTime")).isEqualTo("2026-04-06 10:10:00");
        assertThat(base.get("updateTime")).isEqualTo("2026-04-06 10:30:00");
    }
}
