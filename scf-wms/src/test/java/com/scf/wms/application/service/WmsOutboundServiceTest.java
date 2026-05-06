package com.scf.wms.application.service;

import com.scf.common.exception.BusinessException;
import com.scf.wms.application.service.impl.WmsOutboundServiceImpl;
import com.scf.wms.interfaces.dto.OutboundCreateRequest;
import com.scf.wms.interfaces.dto.TaskActionRequest;
import com.scf.wms.interfaces.dto.TaskShipRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@Import(WmsOutboundServiceImpl.class)
class WmsOutboundServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WmsOutboundService wmsOutboundService;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS oms_order_log");
        jdbcTemplate.execute("DROP TABLE IF EXISTS wms_outbound_task_log");
        jdbcTemplate.execute("DROP TABLE IF EXISTS wms_outbound_task_item");
        jdbcTemplate.execute("DROP TABLE IF EXISTS wms_outbound_task");
        jdbcTemplate.execute("DROP TABLE IF EXISTS oms_order_item");
        jdbcTemplate.execute("DROP TABLE IF EXISTS oms_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS warehouse");

        jdbcTemplate.execute("CREATE TABLE warehouse (warehouse_code VARCHAR(64) PRIMARY KEY, warehouse_name VARCHAR(128) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE oms_order (order_no VARCHAR(64) PRIMARY KEY, status INT NOT NULL, status_text VARCHAR(64) NOT NULL, warehouse_code VARCHAR(64), warehouse_name VARCHAR(128), logistics_provider VARCHAR(64), logistics_provider_name VARCHAR(128), tracking_number VARCHAR(64), dispatch_time TIMESTAMP NULL, outbound_time TIMESTAMP NULL, update_time TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE oms_order_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(64) NOT NULL, sku_code VARCHAR(64) NOT NULL, sku_name VARCHAR(128) NOT NULL, quantity INT NOT NULL, unit_price DECIMAL(12,2) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE oms_order_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(64) NOT NULL, log_time TIMESTAMP NOT NULL, node_code VARCHAR(32) NOT NULL, action_code VARCHAR(64) NOT NULL, operator_name VARCHAR(64) NOT NULL, remark VARCHAR(255), created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE wms_outbound_task (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_no VARCHAR(64) NOT NULL UNIQUE, order_no VARCHAR(64) NOT NULL UNIQUE, warehouse_code VARCHAR(64) NOT NULL, warehouse_name VARCHAR(128) NOT NULL, receiver_name VARCHAR(64) NOT NULL, receiver_phone VARCHAR(32) NOT NULL, receiver_address VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL, status_text VARCHAR(64) NOT NULL, total_qty INT NOT NULL, total_sku_count INT NOT NULL, logistics_provider VARCHAR(64), logistics_provider_name VARCHAR(128), tracking_number VARCHAR(64), created_at TIMESTAMP NOT NULL, picked_at TIMESTAMP NULL, shipped_at TIMESTAMP NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE wms_outbound_task_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_no VARCHAR(64) NOT NULL, order_no VARCHAR(64) NOT NULL, sku_code VARCHAR(64) NOT NULL, sku_name VARCHAR(128) NOT NULL, quantity INT NOT NULL, picked_quantity INT NOT NULL, shipped_quantity INT NOT NULL, unit_price DECIMAL(12,2) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE wms_outbound_task_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_no VARCHAR(64) NOT NULL, log_time TIMESTAMP NOT NULL, action_code VARCHAR(64) NOT NULL, operator_name VARCHAR(64) NOT NULL, remark VARCHAR(255), created_at TIMESTAMP NOT NULL)");

        jdbcTemplate.update("INSERT INTO warehouse (warehouse_code, warehouse_name) VALUES (?, ?)", "WH-WH-01", "Wuhan DC");
        jdbcTemplate.update("INSERT INTO oms_order (order_no, status, status_text, warehouse_code, warehouse_name, logistics_provider, logistics_provider_name, tracking_number, dispatch_time, outbound_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP)",
                "FO1001", 20, "Routed", "", "", "", "", "");
        jdbcTemplate.update("INSERT INTO oms_order_item (order_no, sku_code, sku_name, quantity, unit_price) VALUES (?, ?, ?, ?, ?)",
                "FO1001", "SKU-1", "Test SKU", 2, 19.90);
    }

    @Test
    void createOutboundTaskCreatesTaskItemsAndOmsLogs() {
        boolean result = wmsOutboundService.createOutboundTask(buildCreateRequest());

        assertTrue(result);
        Integer taskCount = jdbcTemplate.queryForObject("select count(1) from wms_outbound_task where order_no = ?", Integer.class, "FO1001");
        Integer itemCount = jdbcTemplate.queryForObject("select count(1) from wms_outbound_task_item where order_no = ?", Integer.class, "FO1001");
        String orderStatus = jdbcTemplate.queryForObject("select status_text from oms_order where order_no = ?", String.class, "FO1001");
        Integer logCount = jdbcTemplate.queryForObject("select count(1) from oms_order_log where order_no = ?", Integer.class, "FO1001");

        assertEquals(1, taskCount);
        assertEquals(1, itemCount);
        assertEquals("Dispatched", orderStatus);
        assertEquals(1, logCount);
    }

    @Test
    void createOutboundTaskIsIdempotentForSameOrder() {
        assertTrue(wmsOutboundService.createOutboundTask(buildCreateRequest()));
        assertTrue(wmsOutboundService.createOutboundTask(buildCreateRequest()));

        Integer taskCount = jdbcTemplate.queryForObject("select count(1) from wms_outbound_task where order_no = ?", Integer.class, "FO1001");
        assertEquals(1, taskCount);
    }

    @Test
    void pickAndShipTaskUpdatesStatusesAndTracking() {
        wmsOutboundService.createOutboundTask(buildCreateRequest());
        String taskNo = jdbcTemplate.queryForObject("select task_no from wms_outbound_task where order_no = ?", String.class, "FO1001");

        TaskActionRequest pickRequest = new TaskActionRequest();
        pickRequest.setOperator("picker");
        Map<String, Object> picked = wmsOutboundService.pickTask(taskNo, pickRequest);
        assertEquals("picked", ((Map<?, ?>) picked.get("base")).get("status"));

        TaskShipRequest shipRequest = new TaskShipRequest();
        shipRequest.setOperator("shipper");
        shipRequest.setLogisticsProvider("sf");
        shipRequest.setLogisticsProviderName("SF Express");
        shipRequest.setTrackingNumber("SF123");
        Map<String, Object> shipped = wmsOutboundService.shipTask(taskNo, shipRequest);

        assertEquals("shipped", ((Map<?, ?>) shipped.get("base")).get("status"));
        assertEquals("Shipped", jdbcTemplate.queryForObject("select status_text from oms_order where order_no = ?", String.class, "FO1001"));
        assertEquals("SF123", jdbcTemplate.queryForObject("select tracking_number from oms_order where order_no = ?", String.class, "FO1001"));
    }

    @Test
    void createOutboundTaskRejectsInvalidRequest() {
        OutboundCreateRequest request = new OutboundCreateRequest();
        request.setOrderId("FO1001");

        assertThrows(BusinessException.class, () -> wmsOutboundService.createOutboundTask(request));
    }

    @Test
    void dashboardAndDetailReturnStructuredData() {
        wmsOutboundService.createOutboundTask(buildCreateRequest());
        String taskNo = jdbcTemplate.queryForObject("select task_no from wms_outbound_task where order_no = ?", String.class, "FO1001");

        Map<String, Object> detail = wmsOutboundService.getTask(taskNo);
        Map<String, Object> dashboard = wmsOutboundService.dashboard();

        assertNotNull(detail.get("base"));
        assertFalse(((List<?>) detail.get("items")).isEmpty());
        assertFalse(((List<?>) dashboard.get("cards")).isEmpty());
    }

    private OutboundCreateRequest buildCreateRequest() {
        OutboundCreateRequest request = new OutboundCreateRequest();
        request.setOrderId("FO1001");
        request.setWarehouseId("WH-WH-01");

        OutboundCreateRequest.ReceiverInfo receiverInfo = new OutboundCreateRequest.ReceiverInfo();
        receiverInfo.setName("Alice");
        receiverInfo.setPhone("13800138000");
        receiverInfo.setAddress("Optics Valley Road 188");
        request.setReceiverInfo(receiverInfo);

        OutboundCreateRequest.OutboundItem item = new OutboundCreateRequest.OutboundItem();
        item.setSkuCode("SKU-1");
        item.setQuantity(2);
        request.setItems(List.of(item));
        return request;
    }
}
