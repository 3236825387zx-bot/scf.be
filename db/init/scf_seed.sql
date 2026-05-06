-- SCF demo database bootstrap
-- Target: MySQL 8.x
-- Usage:
--   1. CREATE DATABASE scf_oms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   2. USE scf_oms;
--   3. SOURCE db/init/scf_seed.sql;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS split_merge_request_order_rel;
DROP TABLE IF EXISTS split_merge_request;
DROP TABLE IF EXISTS oms_exception_action_log;
DROP TABLE IF EXISTS oms_exception_ticket;
DROP TABLE IF EXISTS oms_rule_condition;
DROP TABLE IF EXISTS oms_rule;
DROP TABLE IF EXISTS lgs_callback_record;
DROP TABLE IF EXISTS oms_order_log;
DROP TABLE IF EXISTS wms_outbound_task_log;
DROP TABLE IF EXISTS wms_outbound_task_item;
DROP TABLE IF EXISTS wms_outbound_task;
DROP TABLE IF EXISTS wms_shipment_record;
DROP TABLE IF EXISTS wms_packing_order;
DROP TABLE IF EXISTS wms_picking_task;
DROP TABLE IF EXISTS wms_wave;
DROP TABLE IF EXISTS lgs_parcel_track;
DROP TABLE IF EXISTS lgs_parcel;
DROP TABLE IF EXISTS lgs_provider;
DROP TABLE IF EXISTS oms_order_item;
DROP TABLE IF EXISTS oms_order;
DROP TABLE IF EXISTS upstream_order_item;
DROP TABLE IF EXISTS upstream_order;
DROP TABLE IF EXISTS stock_inventory;
DROP TABLE IF EXISTS warehouse;
DROP TABLE IF EXISTS scf_user;

CREATE TABLE scf_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code       VARCHAR(64) NOT NULL UNIQUE,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password        VARCHAR(128) NOT NULL,
    display_name    VARCHAR(128) NOT NULL,
    role_code       VARCHAR(64) NOT NULL,
    role_name       VARCHAR(128) NOT NULL,
    status          TINYINT NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE warehouse (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code      VARCHAR(64) NOT NULL UNIQUE,
    warehouse_name      VARCHAR(128) NOT NULL,
    warehouse_type      VARCHAR(64) NOT NULL DEFAULT 'dc',
    contact_name        VARCHAR(64) NULL,
    contact_phone       VARCHAR(32) NULL,
    province            VARCHAR(64) NULL,
    city                VARCHAR(64) NULL,
    district            VARCHAR(64) NULL,
    detail_address      VARCHAR(255) NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'enabled',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_inventory (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code      VARCHAR(64) NOT NULL,
    sku_code            VARCHAR(64) NOT NULL,
    sku_name            VARCHAR(128) NOT NULL,
    temp_layer          VARCHAR(64) NOT NULL DEFAULT 'ambient',
    available_quantity  INT NOT NULL DEFAULT 0,
    locked_quantity     INT NOT NULL DEFAULT 0,
    total_quantity      INT NOT NULL DEFAULT 0,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_wh_sku (warehouse_code, sku_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE upstream_order (
    id                      BIGINT PRIMARY KEY,
    upstream_order_no       VARCHAR(64) NOT NULL UNIQUE,
    external_no             VARCHAR(64) NOT NULL UNIQUE,
    channel_code            VARCHAR(64) NOT NULL,
    receiver_name           VARCHAR(64) NOT NULL,
    receiver_phone          VARCHAR(32) NOT NULL,
    province                VARCHAR(64) NULL,
    city                    VARCHAR(64) NULL,
    district                VARCHAR(64) NULL,
    detail_address          VARCHAR(255) NULL,
    temp_layer              VARCHAR(64) NULL,
    requested_delivery      VARCHAR(64) NULL,
    total_amount            DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    remark                  VARCHAR(255) NULL,
    status                  VARCHAR(32) NOT NULL,
    status_text             VARCHAR(64) NOT NULL,
    dispatch_time           DATETIME NULL,
    fulfillment_order_no    VARCHAR(64) NULL,
    target_warehouse_name   VARCHAR(128) NULL,
    create_time             DATETIME NOT NULL,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE upstream_order_item (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    upstream_order_no   VARCHAR(64) NOT NULL,
    sku_code            VARCHAR(64) NOT NULL,
    sku_name            VARCHAR(128) NOT NULL,
    temp_layer          VARCHAR(64) NOT NULL DEFAULT 'ambient',
    quantity            INT NOT NULL,
    unit_price          DECIMAL(12, 2) NOT NULL,
    amount              DECIMAL(12, 2) NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_upstream_order_item_no (upstream_order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE oms_order (
    id                      BIGINT PRIMARY KEY,
    order_no                VARCHAR(64) NOT NULL UNIQUE,
    external_no             VARCHAR(64) NOT NULL,
    parent_id               BIGINT NULL,
    status                  INT NOT NULL,
    status_text             VARCHAR(64) NOT NULL,
    receiver_name           VARCHAR(64) NOT NULL,
    receiver_phone          VARCHAR(32) NOT NULL,
    province                VARCHAR(64) NULL,
    city                    VARCHAR(64) NULL,
    district                VARCHAR(64) NULL,
    detail_address          VARCHAR(255) NULL,
    warehouse_code          VARCHAR(64) NULL,
    warehouse_name          VARCHAR(128) NULL,
    logistics_provider      VARCHAR(64) NULL,
    logistics_provider_name VARCHAR(128) NULL,
    tracking_number         VARCHAR(64) NULL,
    total_amount            DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    route_reason            VARCHAR(255) NULL,
    split_remark            VARCHAR(255) NULL,
    intercept_status        VARCHAR(32) NOT NULL DEFAULT 'none',
    version_no              INT NOT NULL DEFAULT 1,
    create_time             DATETIME NOT NULL,
    dispatch_time           DATETIME NULL,
    outbound_time           DATETIME NULL,
    update_time             DATETIME NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE oms_order_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(64) NOT NULL,
    sku_code        VARCHAR(64) NOT NULL,
    sku_name        VARCHAR(128) NOT NULL,
    temp_layer      VARCHAR(64) NOT NULL DEFAULT 'ambient',
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12, 2) NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_oms_order_item_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE oms_order_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(64) NOT NULL,
    log_time        DATETIME NOT NULL,
    node_code       VARCHAR(32) NOT NULL,
    action_code     VARCHAR(64) NOT NULL,
    operator_name   VARCHAR(64) NOT NULL,
    remark          VARCHAR(255) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_oms_order_log_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wms_outbound_task (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no                 VARCHAR(64) NOT NULL UNIQUE,
    order_no                VARCHAR(64) NOT NULL UNIQUE,
    warehouse_code          VARCHAR(64) NOT NULL,
    warehouse_name          VARCHAR(128) NOT NULL,
    receiver_name           VARCHAR(64) NOT NULL,
    receiver_phone          VARCHAR(32) NOT NULL,
    receiver_address        VARCHAR(255) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    status_text             VARCHAR(64) NOT NULL,
    total_qty               INT NOT NULL DEFAULT 0,
    total_sku_count         INT NOT NULL DEFAULT 0,
    logistics_provider      VARCHAR(64) NULL,
    logistics_provider_name VARCHAR(128) NULL,
    tracking_number         VARCHAR(64) NULL,
    created_at              DATETIME NOT NULL,
    picked_at               DATETIME NULL,
    shipped_at              DATETIME NULL,
    updated_at              DATETIME NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wms_outbound_task_item (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no             VARCHAR(64) NOT NULL,
    order_no            VARCHAR(64) NOT NULL,
    sku_code            VARCHAR(64) NOT NULL,
    sku_name            VARCHAR(128) NOT NULL,
    quantity            INT NOT NULL,
    picked_quantity     INT NOT NULL DEFAULT 0,
    shipped_quantity    INT NOT NULL DEFAULT 0,
    unit_price          DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NOT NULL,
    KEY idx_wms_task_item_task_no (task_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wms_outbound_task_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no         VARCHAR(64) NOT NULL,
    log_time        DATETIME NOT NULL,
    action_code     VARCHAR(64) NOT NULL,
    operator_name   VARCHAR(64) NOT NULL,
    remark          VARCHAR(255) NULL,
    created_at      DATETIME NOT NULL,
    KEY idx_wms_task_log_task_no (task_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wms_wave (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    wave_id         VARCHAR(64) NOT NULL UNIQUE,
    warehouse       VARCHAR(128) NOT NULL,
    area            VARCHAR(64) NULL,
    wave_type       VARCHAR(64) NOT NULL,
    orders_count    INT NOT NULL DEFAULT 0,
    units_count     INT NOT NULL DEFAULT 0,
    priority        VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    device          VARCHAR(64) NULL,
    owner           VARCHAR(64) NULL,
    created_at      DATETIME NOT NULL,
    deadline        DATETIME NULL,
    remark          VARCHAR(255) NULL,
    source          VARCHAR(64) NULL,
    updated_at      DATETIME NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wms_picking_task (
    id              BIGINT PRIMARY KEY,
    wave_id         VARCHAR(64) NOT NULL,
    location        VARCHAR(64) NOT NULL,
    sku             VARCHAR(64) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    qty             INT NOT NULL DEFAULT 0,
    picked_qty      INT NOT NULL DEFAULT 0,
    status          VARCHAR(32) NOT NULL,
    operator        VARCHAR(64) NULL,
    updated_at      DATETIME NOT NULL,
    exception_text  VARCHAR(255) NULL,
    remark          VARCHAR(255) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wms_packing_order (
    id              BIGINT PRIMARY KEY,
    package_no      VARCHAR(64) NOT NULL UNIQUE,
    wave_id         VARCHAR(64) NOT NULL,
    sku             VARCHAR(64) NOT NULL,
    scanned         INT NOT NULL DEFAULT 0,
    required_qty    INT NOT NULL DEFAULT 0,
    result          VARCHAR(64) NULL,
    package_status  VARCHAR(32) NOT NULL,
    material        VARCHAR(128) NULL,
    weight          VARCHAR(64) NULL,
    waybill         VARCHAR(64) NULL,
    printer         VARCHAR(64) NULL,
    operator        VARCHAR(64) NULL,
    updated_at      DATETIME NOT NULL,
    remark          VARCHAR(255) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wms_shipment_record (
    id              BIGINT PRIMARY KEY,
    package_id      VARCHAR(64) NOT NULL,
    wave_id         VARCHAR(64) NOT NULL,
    waybill         VARCHAR(64) NULL,
    weight          VARCHAR(64) NULL,
    fee             VARCHAR(64) NULL,
    handover        VARCHAR(32) NOT NULL,
    carrier         VARCHAR(64) NULL,
    dock            VARCHAR(64) NULL,
    handover_time   DATETIME NULL,
    operator        VARCHAR(64) NULL,
    updated_at      DATETIME NOT NULL,
    remark          VARCHAR(255) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE lgs_provider (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_code   VARCHAR(64) NOT NULL UNIQUE,
    provider_name   VARCHAR(128) NOT NULL,
    service_scope   VARCHAR(64) NOT NULL DEFAULT 'nationwide',
    contact_name    VARCHAR(64) NULL,
    contact_phone   VARCHAR(32) NULL,
    priority_no     INT NOT NULL DEFAULT 10,
    status          VARCHAR(32) NOT NULL DEFAULT 'enabled',
    sla_hours       INT NOT NULL DEFAULT 48,
    base_fee        DECIMAL(12, 2) NOT NULL DEFAULT 10.00,
    fee_per_kg      DECIMAL(12, 2) NOT NULL DEFAULT 1.00,
    api_endpoint    VARCHAR(255) NULL,
    remark          VARCHAR(255) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE lgs_parcel (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    parcel_no           VARCHAR(64) NOT NULL UNIQUE,
    order_no            VARCHAR(64) NOT NULL,
    provider_code       VARCHAR(64) NOT NULL,
    provider_name       VARCHAR(128) NOT NULL,
    tracking_number     VARCHAR(64) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    status_text         VARCHAR(64) NOT NULL,
    receiver_name       VARCHAR(64) NOT NULL,
    receiver_phone      VARCHAR(32) NULL,
    receiver_address    VARCHAR(255) NOT NULL,
    signed_by           VARCHAR(64) NULL,
    signed_at           DATETIME NULL,
    delivery_remark     VARCHAR(255) NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lgs_parcel_order_no (order_no),
    KEY idx_lgs_parcel_provider_code (provider_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE lgs_parcel_track (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parcel_no       VARCHAR(64) NOT NULL,
    event_code      VARCHAR(64) NOT NULL,
    event_name      VARCHAR(128) NOT NULL,
    event_time      DATETIME NOT NULL,
    location        VARCHAR(255) NULL,
    operator_name   VARCHAR(64) NULL,
    remark          VARCHAR(255) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_lgs_parcel_track_no (parcel_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE lgs_callback_record (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no            VARCHAR(64) NOT NULL,
    provider_code       VARCHAR(64) NOT NULL,
    callback_type       VARCHAR(64) NOT NULL,
    callback_status     VARCHAR(64) NOT NULL,
    request_payload     TEXT NULL,
    result_payload      TEXT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lgs_callback_order_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE oms_rule (
    id              BIGINT PRIMARY KEY,
    rule_name       VARCHAR(128) NOT NULL,
    rule_type       VARCHAR(64) NOT NULL,
    warehouse_name  VARCHAR(128) NULL,
    priority_no     INT NOT NULL DEFAULT 1,
    status          VARCHAR(32) NOT NULL DEFAULT 'enabled',
    action_text     VARCHAR(255) NULL,
    updated_by      VARCHAR(64) NOT NULL,
    updated_at      DATETIME NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE oms_rule_condition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT NOT NULL,
    field_code      VARCHAR(64) NOT NULL,
    operator_code   VARCHAR(32) NOT NULL,
    field_value     VARCHAR(128) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rule_condition_rule_id (rule_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE split_merge_request (
    id                  BIGINT PRIMARY KEY,
    request_no          VARCHAR(64) NOT NULL UNIQUE,
    request_type        VARCHAR(32) NOT NULL,
    strategy_code       VARCHAR(64) NOT NULL,
    target_warehouse    VARCHAR(128) NULL,
    reason              VARCHAR(255) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    result_summary      VARCHAR(255) NULL,
    operator_name       VARCHAR(64) NOT NULL,
    operation_source    VARCHAR(32) NOT NULL,
    created_at          DATETIME NOT NULL,
    processed_at        DATETIME NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE split_merge_request_order_rel (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no          VARCHAR(64) NOT NULL,
    source_order_no     VARCHAR(64) NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_split_merge_rel_req (request_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE oms_exception_ticket (
    id                  BIGINT PRIMARY KEY,
    order_no            VARCHAR(64) NOT NULL,
    exception_type      VARCHAR(64) NOT NULL,
    severity            VARCHAR(32) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    description         VARCHAR(255) NOT NULL,
    root_cause          VARCHAR(255) NULL,
    recommended_action  VARCHAR(255) NULL,
    operator_name       VARCHAR(64) NOT NULL,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE oms_exception_action_log (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    exception_id        BIGINT NOT NULL,
    action_code         VARCHAR(64) NOT NULL,
    action_result       VARCHAR(255) NULL,
    remark              VARCHAR(255) NULL,
    operator_name       VARCHAR(64) NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_oms_exception_action_exception_id (exception_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO scf_user (id, user_code, username, password, display_name, role_code, role_name, status, created_at, updated_at) VALUES
(1001, 'user-1001', 'operator', '123456', 'SCF Operator', 'operations', 'operations', 1, '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
(1002, 'user-1002', 'planner', '123456', 'Route Planner', 'planner', 'planner', 1, '2026-03-20 09:05:00', '2026-03-20 09:05:00'),
(1003, 'user-1003', 'admin', '123456', 'SCF Admin', 'admin', 'admin', 1, '2026-03-20 09:10:00', '2026-03-20 09:10:00');

INSERT INTO warehouse (id, warehouse_code, warehouse_name, warehouse_type, contact_name, contact_phone, province, city, district, detail_address, status) VALUES
(1, 'WH-WH-01', 'Wuhan DC', 'dc', 'Liu Wei', '13800000001', 'Hubei', 'Wuhan', 'Hongshan', 'Optics Valley Road 188', 'enabled'),
(2, 'WH-CS-01', 'Changsha Hub', 'dc', 'Chen Bo', '13800000002', 'Hunan', 'Changsha', 'Yuelu', 'Meixi Lake 16', 'enabled'),
(3, 'WH-NJ-01', 'Nanjing Hub', 'dc', 'Zhou Ming', '13800000003', 'Jiangsu', 'Nanjing', 'Jianye', 'Hexi Avenue 88', 'enabled');

INSERT INTO stock_inventory (warehouse_code, sku_code, sku_name, temp_layer, available_quantity, locked_quantity, total_quantity) VALUES
('WH-WH-01', 'SKU-100861', 'Ambient Gift Box', 'ambient', 100, 5, 105),
('WH-WH-01', 'SKU-200001', 'Ambient Milk Box', 'ambient', 220, 10, 230),
('WH-WH-01', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 40, 2, 42),
('WH-CS-01', 'SKU-100861', 'Ambient Gift Box', 'ambient', 80, 3, 83),
('WH-CS-01', 'SKU-200001', 'Ambient Milk Box', 'ambient', 160, 8, 168),
('WH-CS-01', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 120, 6, 126),
('WH-NJ-01', 'SKU-100861', 'Ambient Gift Box', 'ambient', 66, 0, 66),
('WH-NJ-01', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 55, 1, 56);

INSERT INTO upstream_order (
    id, upstream_order_no, external_no, channel_code, receiver_name, receiver_phone,
    province, city, district, detail_address, temp_layer, requested_delivery,
    total_amount, remark, status, status_text, dispatch_time, fulfillment_order_no,
    target_warehouse_name, create_time, updated_at
) VALUES
(9001, 'UP202603220001', 'EC202603220001', 'ecommerce', 'Alice', '13800112233',
 'Hubei', 'Wuhan', 'Hongshan', 'Optics Valley Road 188', 'ambient', 'same_day',
 899.00, '', 'pending', 'Pending', NULL, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:20:00'),
(9002, 'UP202603220002', 'EC202603220002', 'retail', 'Bob', '13900112244',
 'Hunan', 'Changsha', 'Yuelu', 'Meixi Lake 16', 'cold_chain', 'next_day',
 256.00, '', 'dispatched', 'Dispatched', '2026-03-27 10:00:00', 'FO202603220002', 'Changsha Hub', '2026-03-22 11:30:00', '2026-03-27 10:00:00'),
(9003, 'UP202603220003', 'EC202603220003', 'ecommerce', 'Cindy', '13700112255',
 'Jiangsu', 'Nanjing', 'Jianye', 'Hexi Avenue 88', 'ambient', 'normal',
 512.00, 'VIP customer', 'pending', 'Pending', NULL, NULL, NULL, '2026-03-23 15:10:00', '2026-03-23 15:10:00');

INSERT INTO upstream_order_item (upstream_order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('UP202603220001', 'SKU-100861', 'Ambient Gift Box', 'ambient', 3, 199.00, 597.00),
('UP202603220001', 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 151.00, 302.00),
('UP202603220002', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 2, 128.00, 256.00),
('UP202603220003', 'SKU-100861', 'Ambient Gift Box', 'ambient', 1, 256.00, 256.00),
('UP202603220003', 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 128.00, 256.00);

INSERT INTO oms_order (
    id, order_no, external_no, parent_id, status, status_text, receiver_name, receiver_phone,
    province, city, district, detail_address, warehouse_code, warehouse_name,
    logistics_provider, logistics_provider_name, tracking_number, total_amount,
    route_reason, split_remark, intercept_status, version_no, create_time,
    dispatch_time, outbound_time, update_time
) VALUES
(202603220001, 'FO202603220001', 'EC202603220001', NULL, 10, 'Created', 'Alice', '13800112233',
 'Hubei', 'Wuhan', 'Hongshan', 'Optics Valley Road 188', 'WH-WH-01', 'Wuhan DC',
 'sf', 'SF Express', '', 899.00, 'Wuhan region priority', '', 'none', 1,
 '2026-03-22 09:20:00', NULL, NULL, '2026-03-22 09:20:00'),
(202603220002, 'FO202603220002', 'EC202603220002', NULL, 40, 'Dispatched', 'Bob', '13900112244',
 'Hunan', 'Changsha', 'Yuelu', 'Meixi Lake 16', 'WH-CS-01', 'Changsha Hub',
 'jd', 'JD Logistics', 'JDV123456789', 256.00, 'Cold-chain priority', '', 'none', 1,
 '2026-03-22 11:40:00', '2026-03-27 10:00:00', NULL, '2026-03-27 10:00:00'),
(202603220003, 'FO202603220003', 'EC202603220003', NULL, 20, 'Routed', 'Cindy', '13700112255',
 'Jiangsu', 'Nanjing', 'Jianye', 'Hexi Avenue 88', 'WH-NJ-01', 'Nanjing Hub',
 'sf', 'SF Express', '', 512.00, 'Nanjing nearest warehouse', '', 'none', 2,
 '2026-03-23 15:10:00', NULL, NULL, '2026-03-23 15:20:00');

INSERT INTO oms_order_item (order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('FO202603220001', 'SKU-100861', 'Ambient Gift Box', 'ambient', 3, 199.00, 597.00),
('FO202603220001', 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 151.00, 302.00),
('FO202603220002', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 2, 128.00, 256.00),
('FO202603220003', 'SKU-100861', 'Ambient Gift Box', 'ambient', 1, 256.00, 256.00),
('FO202603220003', 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 128.00, 256.00);

INSERT INTO oms_order_log (order_no, log_time, node_code, action_code, operator_name, remark) VALUES
('FO202603220001', '2026-03-22 09:20:00', 'OMS', 'created', 'system', 'order created'),
('FO202603220002', '2026-03-22 11:40:00', 'OMS', 'created', 'system', 'order created'),
('FO202603220002', '2026-03-27 10:00:00', 'OMS', 'dispatched', 'system', 'dispatched to WMS'),
('FO202603220003', '2026-03-23 15:10:00', 'OMS', 'created', 'system', 'order created'),
('FO202603220003', '2026-03-23 15:20:00', 'OMS', 'routed', 'planner', 'assigned to Nanjing hub');

INSERT INTO wms_outbound_task (
    id, task_no, order_no, warehouse_code, warehouse_name, receiver_name, receiver_phone, receiver_address,
    status, status_text, total_qty, total_sku_count, logistics_provider, logistics_provider_name, tracking_number,
    created_at, picked_at, shipped_at, updated_at
) VALUES
(7001, 'WT202603270001', 'FO202603220002', 'WH-CS-01', 'Changsha Hub', 'Bob', '13900112244', 'Meixi Lake 16',
 'shipped', 'Shipped', 2, 1, 'jd', 'JD Logistics', 'JDV123456789',
 '2026-03-27 10:00:00', '2026-03-27 10:20:00', '2026-03-27 12:00:00', '2026-03-27 12:00:00');

INSERT INTO wms_outbound_task_item (
    task_no, order_no, sku_code, sku_name, quantity, picked_quantity, shipped_quantity, unit_price, created_at, updated_at
) VALUES
('WT202603270001', 'FO202603220002', 'SKU-300001', 'Cold Chain Yogurt Set', 2, 2, 2, 128.00, '2026-03-27 10:00:00', '2026-03-27 12:00:00');

INSERT INTO wms_outbound_task_log (task_no, log_time, action_code, operator_name, remark, created_at) VALUES
('WT202603270001', '2026-03-27 10:00:00', 'create', 'system', 'outbound task created', '2026-03-27 10:00:00'),
('WT202603270001', '2026-03-27 10:20:00', 'pick', 'picker-01', 'task picked', '2026-03-27 10:20:00'),
('WT202603270001', '2026-03-27 12:00:00', 'ship', 'shipper-01', 'task shipped', '2026-03-27 12:00:00');

INSERT INTO wms_wave (
    id, wave_id, warehouse, area, wave_type, orders_count, units_count, priority, status, device, owner, created_at, deadline, remark, source, updated_at
) VALUES
(7101, 'W-20391', '深圳中心仓', 'A区', '单品补货', 124, 486, '高', '执行中', 'PDA-07', '周琳', '2026-03-26 08:35:00', '2026-03-26 10:30:00', '优先保障上午揽收车次', '系统生成', '2026-03-26 08:35:00'),
(7102, 'W-20392', '武汉一仓', 'B区', '订单波次', 56, 188, '中', '待复核', 'PDA-03', '张晨', '2026-03-26 09:10:00', '2026-03-26 11:00:00', '午后场次', '手工建波', '2026-03-26 09:10:00'),
(7103, 'W-20393', '长沙协同仓', '冷链区', '冷链优先', 32, 96, '高', '待领取', 'PDA-11', '李敏', '2026-03-26 09:40:00', '2026-03-26 12:00:00', '优先冷链订单', '系统生成', '2026-03-26 09:40:00');

INSERT INTO wms_picking_task (
    id, wave_id, location, sku, name, qty, picked_qty, status, operator, updated_at, exception_text, remark
) VALUES
(7201, 'W-20391', 'A-01-03', 'SKU-100861', '常温饮品组合装', 12, 6, '拣货中', '周琳', '2026-03-26 09:06:00', '', '优先完成整箱位'),
(7202, 'W-20392', 'B-02-01', 'SKU-200001', '常温牛奶箱', 8, 0, '待拣货', '张晨', '2026-03-26 09:12:00', '', '等待波次释放'),
(7203, 'W-20393', 'C-01-08', 'SKU-300001', '冷链酸奶套装', 6, 6, '已完成', '李敏', '2026-03-26 09:28:00', '', '冷链区已完成');

INSERT INTO wms_packing_order (
    id, package_no, wave_id, sku, scanned, required_qty, result, package_status, material, weight, waybill, printer, operator, updated_at, remark
) VALUES
(7301, 'PK202603180021', 'W-20391', 'SKU-100861', 12, 12, '通过', '待打包', '5号纸箱', '8.4kg', 'WB202603180021', 'Zebra-01', '李敏', '2026-03-26 09:28:00', '等待封箱'),
(7302, 'PK202603180022', 'W-20392', 'SKU-200001', 8, 8, '通过', '复核完成', '4号纸箱', '5.2kg', 'WB202603180022', 'Zebra-02', '张晨', '2026-03-26 09:35:00', '等待打包'),
(7303, 'PK202603180023', 'W-20393', 'SKU-300001', 6, 6, '通过', '已打包', '保温箱', '9.1kg', 'WB202603180023', 'Zebra-03', '李敏', '2026-03-26 09:42:00', '冷链包材完成');

INSERT INTO wms_shipment_record (
    id, package_id, wave_id, waybill, weight, fee, handover, carrier, dock, handover_time, operator, updated_at, remark
) VALUES
(7401, 'PK202603180021', 'W-20391', 'WB202603180021', '8.4kg', '26元', '待装车', 'SF', '1号月台', NULL, '李涛', '2026-03-26 10:12:00', '等待承运商到场'),
(7402, 'PK202603180022', 'W-20392', 'WB202603180022', '5.2kg', '18元', '已装车', 'JD', '2号月台', '2026-03-26 10:05:00', '周航', '2026-03-26 10:05:00', '等待交接签收'),
(7403, 'PK202603180023', 'W-20393', 'WB202603180023', '9.1kg', '32元', '已交接', 'SF', '冷链月台', '2026-03-26 10:18:00', '赵凯', '2026-03-26 10:18:00', '冷链车辆签收完成');

INSERT INTO lgs_provider (
    provider_code, provider_name, service_scope, contact_name, contact_phone, priority_no,
    status, sla_hours, base_fee, fee_per_kg, api_endpoint, remark
) VALUES
('sf', 'SF Express', 'nationwide', 'Liu Dispatch', '13800010001', 10, 'enabled', 24, 15.00, 1.50, 'https://mock.sf.example/api', 'default express provider'),
('jd', 'JD Logistics', 'same_city', 'Chen Route', '13800010002', 20, 'enabled', 18, 16.00, 1.20, 'https://mock.jd.example/api', 'same city priority'),
('cold', 'Cold Chain Carrier', 'cold_chain', 'Wang Cold', '13800010003', 30, 'enabled', 36, 28.00, 3.50, 'https://mock.cold.example/api', 'cold chain dedicated');

INSERT INTO lgs_parcel (
    parcel_no, order_no, provider_code, provider_name, tracking_number, status, status_text,
    receiver_name, receiver_phone, receiver_address, signed_by, signed_at, delivery_remark, created_at, updated_at
) VALUES
('LP202603270001', 'FO202603220002', 'jd', 'JD Logistics', 'JDV123456789', 'in_transit', 'In Transit',
 'Bob', '13900112244', 'Hunan Changsha Yuelu Meixi Lake 16', NULL, NULL, 'cold-chain en route', '2026-03-27 12:00:00', '2026-03-27 14:00:00');

INSERT INTO lgs_parcel_track (
    parcel_no, event_code, event_name, event_time, location, operator_name, remark, created_at
) VALUES
('LP202603270001', 'created', 'Parcel Created', '2026-03-27 12:00:00', 'Changsha Hub', 'shipper-01', 'handover to carrier', '2026-03-27 12:00:00'),
('LP202603270001', 'in_transit', 'In Transit', '2026-03-27 14:00:00', 'Changsha Transfer Center', 'route-bot', 'vehicle departed', '2026-03-27 14:00:00');

INSERT INTO lgs_callback_record (
    order_no, provider_code, callback_type, callback_status, request_payload, result_payload, created_at, updated_at
) VALUES
('FO202603220002', 'jd', 'track_sync', 'success', '{"trackingNumber":"JDV123456789"}', '{"status":"in_transit"}', '2026-03-27 14:00:00', '2026-03-27 14:00:00');

INSERT INTO oms_rule (id, rule_name, rule_type, warehouse_name, priority_no, status, action_text, updated_by, updated_at) VALUES
(3001, 'Hubei to Wuhan', 'warehouse_route', 'Wuhan DC', 10, 'enabled', 'assign to Wuhan DC', 'system', '2026-03-27 09:30:00'),
(3002, 'Cold chain to Changsha', 'warehouse_route', 'Changsha Hub', 20, 'enabled', 'assign to Changsha hub', 'system', '2026-03-27 09:45:00'),
(3003, 'Nanjing local priority', 'warehouse_route', 'Nanjing Hub', 30, 'enabled', 'assign to Nanjing hub', 'planner', '2026-03-27 10:15:00');

INSERT INTO oms_rule_condition (rule_id, field_code, operator_code, field_value) VALUES
(3001, 'receiverProvince', 'eq', 'Hubei'),
(3002, 'tempLayer', 'eq', 'cold_chain'),
(3003, 'receiverProvince', 'eq', 'Jiangsu');

INSERT INTO split_merge_request (
    id, request_no, request_type, strategy_code, target_warehouse, reason,
    status, result_summary, operator_name, operation_source, created_at, processed_at
) VALUES
(5001, 'SM202603270001', 'split', 'split_by_temp', 'Changsha Hub', 'split by temp',
 'pending', '', 'operator', 'manual', '2026-03-27 11:00:00', NULL),
(5002, 'SM202603270002', 'merge', 'merge_same_address', 'Wuhan DC', 'merge same consignee orders',
 'done', 'request executed', 'planner', 'manual', '2026-03-27 11:30:00', '2026-03-27 11:40:00');

INSERT INTO split_merge_request_order_rel (request_no, source_order_no) VALUES
('SM202603270001', 'FO202603220002'),
('SM202603270002', 'FO202603220001'),
('SM202603270002', 'FO202603220003');

INSERT INTO oms_exception_ticket (
    id, order_no, exception_type, severity, status, description, root_cause, recommended_action, operator_name, created_at, updated_at
) VALUES
(8001, 'FO202603220001', 'inventory_shortage', 'high', 'open', 'inventory lock pending manual release', 'warehouse stock mismatch', 'release inventory and re-route', 'system', '2026-03-27 13:00:00', '2026-03-27 13:00:00'),
(8002, 'FO202603220002', 'delivery_delay', 'medium', 'processing', 'carrier callback delayed', 'provider callback timeout', 'generate compensation log', 'operator', '2026-03-27 16:00:00', '2026-03-27 16:30:00');

INSERT INTO oms_exception_action_log (
    exception_id, action_code, action_result, remark, operator_name, created_at
) VALUES
(8001, 'create', 'open', 'exception created', 'system', '2026-03-27 13:00:00'),
(8002, 'follow_up', 'processing', 'awaiting carrier response', 'operator', '2026-03-27 16:30:00');

SET FOREIGN_KEY_CHECKS = 1;
