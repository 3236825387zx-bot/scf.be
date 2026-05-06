-- SCF shipped demo data
-- Usage:
--   1. Initialize base schema/data with db/init/scf_seed.sql
--   2. USE scf_oms;
--   3. SOURCE db/init/scf_shipped_demo.sql;
--
-- This script appends fifteen realistic "shipped but not signed" order chains:
-- upstream_order -> oms_order -> wms_outbound_task -> lgs_parcel

SET NAMES utf8mb4;

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- Chain 1: ambient order, Hubei -> Wuhan DC -> SF Express
-- Amount: 398.00 + 387.00 = 785.00
-- Qty: 2 + 3 = 5
-- ---------------------------------------------------------------------------

INSERT INTO upstream_order (
    id, upstream_order_no, external_no, channel_code, receiver_name, receiver_phone,
    province, city, district, detail_address, temp_layer, requested_delivery,
    total_amount, remark, status, status_text, dispatch_time, fulfillment_order_no,
    target_warehouse_name, create_time, updated_at
) VALUES
(9101, 'UP202604080101', 'EC202604080101', 'ecommerce', 'Deng Yan', '13600110001',
 'Hubei', 'Wuhan', 'Hongshan', 'Guanggu 1st Road 88', 'ambient', 'same_day',
 785.00, 'same-day ambient shipment demo', 'dispatched', 'Dispatched', '2026-04-08 09:18:00', 'FO202604080101',
 'Wuhan DC', '2026-04-08 08:30:00', '2026-04-08 11:05:00');

INSERT INTO upstream_order_item (upstream_order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('UP202604080101', 'SKU-100861', 'Ambient Gift Box', 'ambient', 2, 199.00, 398.00),
('UP202604080101', 'SKU-200001', 'Ambient Milk Box', 'ambient', 3, 129.00, 387.00);

INSERT INTO oms_order (
    id, order_no, external_no, parent_id, status, status_text, receiver_name, receiver_phone,
    province, city, district, detail_address, warehouse_code, warehouse_name,
    logistics_provider, logistics_provider_name, tracking_number, total_amount,
    route_reason, split_remark, intercept_status, version_no, create_time,
    dispatch_time, outbound_time, update_time
) VALUES
(202604080101, 'FO202604080101', 'EC202604080101', NULL, 50, 'Shipped', 'Deng Yan', '13600110001',
 'Hubei', 'Wuhan', 'Hongshan', 'Guanggu 1st Road 88', 'WH-WH-01', 'Wuhan DC',
 'sf', 'SF Express', 'SFV202604080101', 785.00,
 'Hubei ambient routed to Wuhan DC', '', 'none', 1, '2026-04-08 08:31:00',
 '2026-04-08 09:18:00', '2026-04-08 11:05:00', '2026-04-08 13:10:00');

INSERT INTO oms_order_item (order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('FO202604080101', 'SKU-100861', 'Ambient Gift Box', 'ambient', 2, 199.00, 398.00),
('FO202604080101', 'SKU-200001', 'Ambient Milk Box', 'ambient', 3, 129.00, 387.00);

INSERT INTO oms_order_log (order_no, log_time, node_code, action_code, operator_name, remark, created_at) VALUES
('FO202604080101', '2026-04-08 08:31:00', 'OMS', 'created', 'system', 'order created', '2026-04-08 08:31:00'),
('FO202604080101', '2026-04-08 08:36:00', 'OMS', 'routed', 'planner', 'assigned to Wuhan DC', '2026-04-08 08:36:00'),
('FO202604080101', '2026-04-08 09:18:00', 'OMS', 'dispatched', 'system', 'dispatched to WMS', '2026-04-08 09:18:00'),
('FO202604080101', '2026-04-08 09:18:00', 'WMS', 'create_task', 'system', 'wms outbound task created: WT202604080101', '2026-04-08 09:18:00'),
('FO202604080101', '2026-04-08 10:02:00', 'WMS', 'picked', 'picker-11', 'task picked', '2026-04-08 10:02:00'),
('FO202604080101', '2026-04-08 11:05:00', 'WMS', 'shipped', 'shipper-11', 'task shipped', '2026-04-08 11:05:00');

INSERT INTO wms_outbound_task (
    id, task_no, order_no, warehouse_code, warehouse_name, receiver_name, receiver_phone, receiver_address,
    status, status_text, total_qty, total_sku_count, logistics_provider, logistics_provider_name, tracking_number,
    created_at, picked_at, shipped_at, updated_at
) VALUES
(7011, 'WT202604080101', 'FO202604080101', 'WH-WH-01', 'Wuhan DC', 'Deng Yan', '13600110001', 'Hubei Wuhan Hongshan Guanggu 1st Road 88',
 'shipped', 'Shipped', 5, 2, 'sf', 'SF Express', 'SFV202604080101',
 '2026-04-08 09:18:00', '2026-04-08 10:02:00', '2026-04-08 11:05:00', '2026-04-08 11:05:00');

INSERT INTO wms_outbound_task_item (
    task_no, order_no, sku_code, sku_name, quantity, picked_quantity, shipped_quantity, unit_price, created_at, updated_at
) VALUES
('WT202604080101', 'FO202604080101', 'SKU-100861', 'Ambient Gift Box', 2, 2, 2, 199.00, '2026-04-08 09:18:00', '2026-04-08 11:05:00'),
('WT202604080101', 'FO202604080101', 'SKU-200001', 'Ambient Milk Box', 3, 3, 3, 129.00, '2026-04-08 09:18:00', '2026-04-08 11:05:00');

INSERT INTO wms_outbound_task_log (task_no, log_time, action_code, operator_name, remark, created_at) VALUES
('WT202604080101', '2026-04-08 09:18:00', 'create', 'system', 'outbound task created', '2026-04-08 09:18:00'),
('WT202604080101', '2026-04-08 10:02:00', 'pick', 'picker-11', 'task picked', '2026-04-08 10:02:00'),
('WT202604080101', '2026-04-08 11:05:00', 'ship', 'shipper-11', 'task shipped', '2026-04-08 11:05:00');

INSERT INTO lgs_parcel (
    parcel_no, order_no, provider_code, provider_name, tracking_number, status, status_text,
    receiver_name, receiver_phone, receiver_address, signed_by, signed_at, delivery_remark, created_at, updated_at
) VALUES
('LP202604080101', 'FO202604080101', 'sf', 'SF Express', 'SFV202604080101', 'in_transit', 'In Transit',
 'Deng Yan', '13600110001', 'Hubei Wuhan Hongshan Guanggu 1st Road 88', NULL, NULL, 'same-day parcel moving to city station', '2026-04-08 11:05:00', '2026-04-08 13:10:00');

INSERT INTO lgs_parcel_track (
    parcel_no, event_code, event_name, event_time, location, operator_name, remark, created_at
) VALUES
('LP202604080101', 'created', 'Parcel Created', '2026-04-08 11:05:00', 'Wuhan DC', 'shipper-11', 'handover to carrier', '2026-04-08 11:05:00'),
('LP202604080101', 'in_transit', 'In Transit', '2026-04-08 13:10:00', 'Wuhan Hongshan Station', 'route-bot', 'vehicle departed for final station', '2026-04-08 13:10:00');

INSERT INTO lgs_callback_record (
    order_no, provider_code, callback_type, callback_status, request_payload, result_payload, created_at, updated_at
) VALUES
('FO202604080101', 'sf', 'track_sync', 'success', '{"trackingNumber":"SFV202604080101","orderNo":"FO202604080101"}', '{"status":"in_transit","location":"Wuhan Hongshan Station"}', '2026-04-08 13:10:00', '2026-04-08 13:10:00');

-- ---------------------------------------------------------------------------
-- Chain 2: cold-chain order, Hunan -> Changsha Hub -> Cold Chain Carrier
-- Amount: 384.00
-- Qty: 3
-- ---------------------------------------------------------------------------

INSERT INTO upstream_order (
    id, upstream_order_no, external_no, channel_code, receiver_name, receiver_phone,
    province, city, district, detail_address, temp_layer, requested_delivery,
    total_amount, remark, status, status_text, dispatch_time, fulfillment_order_no,
    target_warehouse_name, create_time, updated_at
) VALUES
(9102, 'UP202604080102', 'EC202604080102', 'retail', 'He Min', '13600110002',
 'Hunan', 'Changsha', 'Yuelu', 'Meixi Lake Jinxiu Road 66', 'cold_chain', 'next_day',
 384.00, 'cold-chain retail shipment demo', 'dispatched', 'Dispatched', '2026-04-08 10:12:00', 'FO202604080102',
 'Changsha Hub', '2026-04-08 09:05:00', '2026-04-08 12:40:00');

INSERT INTO upstream_order_item (upstream_order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('UP202604080102', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 3, 128.00, 384.00);

INSERT INTO oms_order (
    id, order_no, external_no, parent_id, status, status_text, receiver_name, receiver_phone,
    province, city, district, detail_address, warehouse_code, warehouse_name,
    logistics_provider, logistics_provider_name, tracking_number, total_amount,
    route_reason, split_remark, intercept_status, version_no, create_time,
    dispatch_time, outbound_time, update_time
) VALUES
(202604080102, 'FO202604080102', 'EC202604080102', NULL, 50, 'Shipped', 'He Min', '13600110002',
 'Hunan', 'Changsha', 'Yuelu', 'Meixi Lake Jinxiu Road 66', 'WH-CS-01', 'Changsha Hub',
 'cold', 'Cold Chain Carrier', 'COLD202604080102', 384.00,
 'Cold-chain order routed to Changsha Hub', '', 'none', 1, '2026-04-08 09:06:00',
 '2026-04-08 10:12:00', '2026-04-08 12:40:00', '2026-04-08 14:30:00');

INSERT INTO oms_order_item (order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('FO202604080102', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 3, 128.00, 384.00);

INSERT INTO oms_order_log (order_no, log_time, node_code, action_code, operator_name, remark, created_at) VALUES
('FO202604080102', '2026-04-08 09:06:00', 'OMS', 'created', 'system', 'order created', '2026-04-08 09:06:00'),
('FO202604080102', '2026-04-08 09:11:00', 'OMS', 'routed', 'planner', 'assigned to Changsha Hub for cold-chain fulfillment', '2026-04-08 09:11:00'),
('FO202604080102', '2026-04-08 10:12:00', 'OMS', 'dispatched', 'system', 'dispatched to WMS', '2026-04-08 10:12:00'),
('FO202604080102', '2026-04-08 10:12:00', 'WMS', 'create_task', 'system', 'wms outbound task created: WT202604080102', '2026-04-08 10:12:00'),
('FO202604080102', '2026-04-08 11:08:00', 'WMS', 'picked', 'picker-12', 'cold-chain task picked', '2026-04-08 11:08:00'),
('FO202604080102', '2026-04-08 12:40:00', 'WMS', 'shipped', 'shipper-12', 'cold-chain task shipped', '2026-04-08 12:40:00');

INSERT INTO wms_outbound_task (
    id, task_no, order_no, warehouse_code, warehouse_name, receiver_name, receiver_phone, receiver_address,
    status, status_text, total_qty, total_sku_count, logistics_provider, logistics_provider_name, tracking_number,
    created_at, picked_at, shipped_at, updated_at
) VALUES
(7012, 'WT202604080102', 'FO202604080102', 'WH-CS-01', 'Changsha Hub', 'He Min', '13600110002', 'Hunan Changsha Yuelu Meixi Lake Jinxiu Road 66',
 'shipped', 'Shipped', 3, 1, 'cold', 'Cold Chain Carrier', 'COLD202604080102',
 '2026-04-08 10:12:00', '2026-04-08 11:08:00', '2026-04-08 12:40:00', '2026-04-08 12:40:00');

INSERT INTO wms_outbound_task_item (
    task_no, order_no, sku_code, sku_name, quantity, picked_quantity, shipped_quantity, unit_price, created_at, updated_at
) VALUES
('WT202604080102', 'FO202604080102', 'SKU-300001', 'Cold Chain Yogurt Set', 3, 3, 3, 128.00, '2026-04-08 10:12:00', '2026-04-08 12:40:00');

INSERT INTO wms_outbound_task_log (task_no, log_time, action_code, operator_name, remark, created_at) VALUES
('WT202604080102', '2026-04-08 10:12:00', 'create', 'system', 'outbound task created', '2026-04-08 10:12:00'),
('WT202604080102', '2026-04-08 11:08:00', 'pick', 'picker-12', 'cold-chain task picked', '2026-04-08 11:08:00'),
('WT202604080102', '2026-04-08 12:40:00', 'ship', 'shipper-12', 'cold-chain task shipped', '2026-04-08 12:40:00');

INSERT INTO lgs_parcel (
    parcel_no, order_no, provider_code, provider_name, tracking_number, status, status_text,
    receiver_name, receiver_phone, receiver_address, signed_by, signed_at, delivery_remark, created_at, updated_at
) VALUES
('LP202604080102', 'FO202604080102', 'cold', 'Cold Chain Carrier', 'COLD202604080102', 'in_transit', 'In Transit',
 'He Min', '13600110002', 'Hunan Changsha Yuelu Meixi Lake Jinxiu Road 66', NULL, NULL, 'cold-chain vehicle departed with insulation container', '2026-04-08 12:40:00', '2026-04-08 14:30:00');

INSERT INTO lgs_parcel_track (
    parcel_no, event_code, event_name, event_time, location, operator_name, remark, created_at
) VALUES
('LP202604080102', 'created', 'Parcel Created', '2026-04-08 12:40:00', 'Changsha Hub Cold Room', 'shipper-12', 'handover to cold-chain carrier', '2026-04-08 12:40:00'),
('LP202604080102', 'in_transit', 'In Transit', '2026-04-08 14:30:00', 'Changsha Cold Chain Transit Center', 'route-bot', 'temperature-controlled vehicle departed', '2026-04-08 14:30:00');

INSERT INTO lgs_callback_record (
    order_no, provider_code, callback_type, callback_status, request_payload, result_payload, created_at, updated_at
) VALUES
('FO202604080102', 'cold', 'track_sync', 'success', '{"trackingNumber":"COLD202604080102","orderNo":"FO202604080102"}', '{"status":"in_transit","temperature":"2-6C"}', '2026-04-08 14:30:00', '2026-04-08 14:30:00');

-- ---------------------------------------------------------------------------
-- Chain 3: ambient order, Jiangsu -> Nanjing Hub -> SF Express
-- Amount: 256.00 + 248.00 = 504.00
-- Qty: 1 + 2 = 3
-- ---------------------------------------------------------------------------

INSERT INTO upstream_order (
    id, upstream_order_no, external_no, channel_code, receiver_name, receiver_phone,
    province, city, district, detail_address, temp_layer, requested_delivery,
    total_amount, remark, status, status_text, dispatch_time, fulfillment_order_no,
    target_warehouse_name, create_time, updated_at
) VALUES
(9103, 'UP202604080103', 'EC202604080103', 'ecommerce', 'Lin Qiao', '13600110003',
 'Jiangsu', 'Nanjing', 'Jianye', 'Hexi New Town Yurun Street 18', 'ambient', 'normal',
 504.00, 'jiangsu routed shipment demo', 'dispatched', 'Dispatched', '2026-04-08 14:15:00', 'FO202604080103',
 'Nanjing Hub', '2026-04-08 13:20:00', '2026-04-08 17:10:00');

INSERT INTO upstream_order_item (upstream_order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('UP202604080103', 'SKU-100861', 'Ambient Gift Box', 'ambient', 1, 256.00, 256.00),
('UP202604080103', 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 124.00, 248.00);

INSERT INTO oms_order (
    id, order_no, external_no, parent_id, status, status_text, receiver_name, receiver_phone,
    province, city, district, detail_address, warehouse_code, warehouse_name,
    logistics_provider, logistics_provider_name, tracking_number, total_amount,
    route_reason, split_remark, intercept_status, version_no, create_time,
    dispatch_time, outbound_time, update_time
) VALUES
(202604080103, 'FO202604080103', 'EC202604080103', NULL, 50, 'Shipped', 'Lin Qiao', '13600110003',
 'Jiangsu', 'Nanjing', 'Jianye', 'Hexi New Town Yurun Street 18', 'WH-NJ-01', 'Nanjing Hub',
 'sf', 'SF Express', 'SFV202604080103', 504.00,
 'Jiangsu local order routed to Nanjing Hub', '', 'none', 2, '2026-04-08 13:21:00',
 '2026-04-08 14:15:00', '2026-04-08 17:10:00', '2026-04-08 18:40:00');

INSERT INTO oms_order_item (order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount) VALUES
('FO202604080103', 'SKU-100861', 'Ambient Gift Box', 'ambient', 1, 256.00, 256.00),
('FO202604080103', 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 124.00, 248.00);

INSERT INTO oms_order_log (order_no, log_time, node_code, action_code, operator_name, remark, created_at) VALUES
('FO202604080103', '2026-04-08 13:21:00', 'OMS', 'created', 'system', 'order created', '2026-04-08 13:21:00'),
('FO202604080103', '2026-04-08 13:28:00', 'OMS', 'routed', 'planner', 'assigned to Nanjing Hub', '2026-04-08 13:28:00'),
('FO202604080103', '2026-04-08 14:15:00', 'OMS', 'dispatched', 'system', 'dispatched to WMS', '2026-04-08 14:15:00'),
('FO202604080103', '2026-04-08 14:15:00', 'WMS', 'create_task', 'system', 'wms outbound task created: WT202604080103', '2026-04-08 14:15:00'),
('FO202604080103', '2026-04-08 15:06:00', 'WMS', 'picked', 'picker-13', 'task picked', '2026-04-08 15:06:00'),
('FO202604080103', '2026-04-08 17:10:00', 'WMS', 'shipped', 'shipper-13', 'task shipped', '2026-04-08 17:10:00');

INSERT INTO wms_outbound_task (
    id, task_no, order_no, warehouse_code, warehouse_name, receiver_name, receiver_phone, receiver_address,
    status, status_text, total_qty, total_sku_count, logistics_provider, logistics_provider_name, tracking_number,
    created_at, picked_at, shipped_at, updated_at
) VALUES
(7013, 'WT202604080103', 'FO202604080103', 'WH-NJ-01', 'Nanjing Hub', 'Lin Qiao', '13600110003', 'Jiangsu Nanjing Jianye Hexi New Town Yurun Street 18',
 'shipped', 'Shipped', 3, 2, 'sf', 'SF Express', 'SFV202604080103',
 '2026-04-08 14:15:00', '2026-04-08 15:06:00', '2026-04-08 17:10:00', '2026-04-08 17:10:00');

INSERT INTO wms_outbound_task_item (
    task_no, order_no, sku_code, sku_name, quantity, picked_quantity, shipped_quantity, unit_price, created_at, updated_at
) VALUES
('WT202604080103', 'FO202604080103', 'SKU-100861', 'Ambient Gift Box', 1, 1, 1, 256.00, '2026-04-08 14:15:00', '2026-04-08 17:10:00'),
('WT202604080103', 'FO202604080103', 'SKU-200001', 'Ambient Milk Box', 2, 2, 2, 124.00, '2026-04-08 14:15:00', '2026-04-08 17:10:00');

INSERT INTO wms_outbound_task_log (task_no, log_time, action_code, operator_name, remark, created_at) VALUES
('WT202604080103', '2026-04-08 14:15:00', 'create', 'system', 'outbound task created', '2026-04-08 14:15:00'),
('WT202604080103', '2026-04-08 15:06:00', 'pick', 'picker-13', 'task picked', '2026-04-08 15:06:00'),
('WT202604080103', '2026-04-08 17:10:00', 'ship', 'shipper-13', 'task shipped', '2026-04-08 17:10:00');

INSERT INTO lgs_parcel (
    parcel_no, order_no, provider_code, provider_name, tracking_number, status, status_text,
    receiver_name, receiver_phone, receiver_address, signed_by, signed_at, delivery_remark, created_at, updated_at
) VALUES
('LP202604080103', 'FO202604080103', 'sf', 'SF Express', 'SFV202604080103', 'in_transit', 'In Transit',
 'Lin Qiao', '13600110003', 'Jiangsu Nanjing Jianye Hexi New Town Yurun Street 18', NULL, NULL, 'parcel left Nanjing hub for district station', '2026-04-08 17:10:00', '2026-04-08 18:40:00');

INSERT INTO lgs_parcel_track (
    parcel_no, event_code, event_name, event_time, location, operator_name, remark, created_at
) VALUES
('LP202604080103', 'created', 'Parcel Created', '2026-04-08 17:10:00', 'Nanjing Hub', 'shipper-13', 'handover to carrier', '2026-04-08 17:10:00'),
('LP202604080103', 'in_transit', 'In Transit', '2026-04-08 18:40:00', 'Nanjing Jianye Station', 'route-bot', 'vehicle departed for local station', '2026-04-08 18:40:00');

INSERT INTO lgs_callback_record (
    order_no, provider_code, callback_type, callback_status, request_payload, result_payload, created_at, updated_at
) VALUES
('FO202604080103', 'sf', 'track_sync', 'success', '{"trackingNumber":"SFV202604080103","orderNo":"FO202604080103"}', '{"status":"in_transit","location":"Nanjing Jianye Station"}', '2026-04-08 18:40:00', '2026-04-08 18:40:00');

-- ---------------------------------------------------------------------------
-- Bulk add 12 more shipped chains so the demo volume reaches about 5x
-- ---------------------------------------------------------------------------

CREATE TEMPORARY TABLE demo_shipped_chain_seed (
    seq INT PRIMARY KEY,
    upstream_id BIGINT NOT NULL,
    oms_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    upstream_order_no VARCHAR(64) NOT NULL,
    external_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    parcel_no VARCHAR(64) NOT NULL,
    tracking_number VARCHAR(64) NOT NULL,
    channel_code VARCHAR(64) NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    receiver_phone VARCHAR(32) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    temp_layer VARCHAR(64) NOT NULL,
    requested_delivery VARCHAR(64) NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    warehouse_name VARCHAR(128) NOT NULL,
    logistics_provider VARCHAR(64) NOT NULL,
    logistics_provider_name VARCHAR(128) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    total_qty INT NOT NULL,
    total_sku_count INT NOT NULL,
    remark VARCHAR(255) NOT NULL,
    route_reason VARCHAR(255) NOT NULL,
    delivery_remark VARCHAR(255) NOT NULL,
    transit_location VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL,
    routed_time DATETIME NOT NULL,
    dispatch_time DATETIME NOT NULL,
    picked_at DATETIME NOT NULL,
    shipped_at DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    picker_name VARCHAR(64) NOT NULL,
    shipper_name VARCHAR(64) NOT NULL,
    item1_sku_code VARCHAR(64) NOT NULL,
    item1_sku_name VARCHAR(128) NOT NULL,
    item1_temp_layer VARCHAR(64) NOT NULL,
    item1_qty INT NOT NULL,
    item1_unit_price DECIMAL(12, 2) NOT NULL,
    item1_amount DECIMAL(12, 2) NOT NULL,
    item2_sku_code VARCHAR(64) NULL,
    item2_sku_name VARCHAR(128) NULL,
    item2_temp_layer VARCHAR(64) NULL,
    item2_qty INT NULL,
    item2_unit_price DECIMAL(12, 2) NULL,
    item2_amount DECIMAL(12, 2) NULL
);

INSERT INTO demo_shipped_chain_seed (
    seq, upstream_id, oms_id, task_id, upstream_order_no, external_no, order_no, task_no, parcel_no, tracking_number,
    channel_code, receiver_name, receiver_phone, province, city, district, detail_address, temp_layer, requested_delivery,
    warehouse_code, warehouse_name, logistics_provider, logistics_provider_name, total_amount, total_qty, total_sku_count,
    remark, route_reason, delivery_remark, transit_location, create_time, routed_time, dispatch_time, picked_at, shipped_at,
    update_time, picker_name, shipper_name, item1_sku_code, item1_sku_name, item1_temp_layer, item1_qty, item1_unit_price,
    item1_amount, item2_sku_code, item2_sku_name, item2_temp_layer, item2_qty, item2_unit_price, item2_amount
) VALUES
(104, 9104, 202604090104, 7014, 'UP202604090104', 'EC202604090104', 'FO202604090104', 'WT202604090104', 'LP202604090104', 'SFV202604090104',
 'ecommerce', 'Qin Lan', '13600110104', 'Hubei', 'Wuhan', 'Jianghan', 'Jiefang Avenue 188', 'ambient', 'same_day',
 'WH-WH-01', 'Wuhan DC', 'sf', 'SF Express', 894.00, 6, 2,
 'same-day ambient shipment expansion', 'Wuhan urban order routed to Wuhan DC', 'parcel left Wuhan DC for Jianghan station', 'Wuhan Jianghan Station',
 '2026-04-09 08:05:00', '2026-04-09 08:11:00', '2026-04-09 08:48:00', '2026-04-09 09:26:00', '2026-04-09 10:18:00',
 '2026-04-09 12:05:00', 'picker-21', 'shipper-21', 'SKU-100861', 'Ambient Gift Box', 'ambient', 2, 205.00,
 410.00, 'SKU-200001', 'Ambient Milk Box', 'ambient', 4, 121.00, 484.00),
(105, 9105, 202604090105, 7015, 'UP202604090105', 'EC202604090105', 'FO202604090105', 'WT202604090105', 'LP202604090105', 'COLD202604090105',
 'retail', 'Song Rui', '13600110105', 'Hunan', 'Changsha', 'Yuelu', 'Jinxing North Road 66', 'cold_chain', 'next_day',
 'WH-CS-01', 'Changsha Hub', 'cold', 'Cold Chain Carrier', 504.00, 4, 1,
 'cold-chain shipment expansion', 'Cold-chain order routed to Changsha Hub', 'cold-chain vehicle departed with insulated container', 'Changsha Cold Chain Transit Center',
 '2026-04-09 08:20:00', '2026-04-09 08:28:00', '2026-04-09 09:02:00', '2026-04-09 10:06:00', '2026-04-09 11:12:00',
 '2026-04-09 13:10:00', 'picker-22', 'shipper-22', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 4, 126.00,
 504.00, NULL, NULL, NULL, NULL, NULL, NULL),
(106, 9106, 202604090106, 7016, 'UP202604090106', 'EC202604090106', 'FO202604090106', 'WT202604090106', 'LP202604090106', 'SFV202604090106',
 'ecommerce', 'Guo Xin', '13600110106', 'Jiangsu', 'Nanjing', 'Qinhuai', 'Taiping South Road 28', 'ambient', 'normal',
 'WH-NJ-01', 'Nanjing Hub', 'sf', 'SF Express', 512.00, 3, 2,
 'nanjing ambient shipment expansion', 'Nanjing local order routed to Nanjing Hub', 'parcel moved from Nanjing hub to Qinhuai station', 'Nanjing Qinhuai Station',
 '2026-04-09 08:42:00', '2026-04-09 08:49:00', '2026-04-09 09:30:00', '2026-04-09 10:18:00', '2026-04-09 11:40:00',
 '2026-04-09 13:22:00', 'picker-23', 'shipper-23', 'SKU-100861', 'Ambient Gift Box', 'ambient', 2, 188.00,
 376.00, 'SKU-200001', 'Ambient Milk Box', 'ambient', 1, 136.00, 136.00),
(107, 9107, 202604090107, 7017, 'UP202604090107', 'EC202604090107', 'FO202604090107', 'WT202604090107', 'LP202604090107', 'SFV202604090107',
 'ecommerce', 'Zhou Fei', '13600110107', 'Hubei', 'Wuhan', 'Hongshan', 'Luoyu Road 520', 'ambient', 'same_day',
 'WH-WH-01', 'Wuhan DC', 'sf', 'SF Express', 800.00, 5, 2,
 'hongshan same-day shipment expansion', 'Wuhan ambient order routed to Wuhan DC', 'same-day parcel heading to Hongshan station', 'Wuhan Hongshan Station',
 '2026-04-09 09:05:00', '2026-04-09 09:11:00', '2026-04-09 09:45:00', '2026-04-09 10:24:00', '2026-04-09 11:26:00',
 '2026-04-09 13:06:00', 'picker-24', 'shipper-24', 'SKU-100861', 'Ambient Gift Box', 'ambient', 3, 178.00,
 534.00, 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 133.00, 266.00),
(108, 9108, 202604090108, 7018, 'UP202604090108', 'EC202604090108', 'FO202604090108', 'WT202604090108', 'LP202604090108', 'COLD202604090108',
 'retail', 'Shen Yue', '13600110108', 'Hubei', 'Wuhan', 'Wuchang', 'Youyi Avenue 72', 'cold_chain', 'next_day',
 'WH-WH-01', 'Wuhan DC', 'cold', 'Cold Chain Carrier', 292.00, 2, 1,
 'wuhan cold-chain shipment expansion', 'Cold-chain order routed to Wuhan DC cold area', 'insulated parcel departed for Wuchang route', 'Wuhan Cold Chain Transit Center',
 '2026-04-09 09:18:00', '2026-04-09 09:25:00', '2026-04-09 10:02:00', '2026-04-09 10:56:00', '2026-04-09 12:05:00',
 '2026-04-09 13:44:00', 'picker-25', 'shipper-25', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 2, 146.00,
 292.00, NULL, NULL, NULL, NULL, NULL, NULL),
(109, 9109, 202604090109, 7019, 'UP202604090109', 'EC202604090109', 'FO202604090109', 'WT202604090109', 'LP202604090109', 'SFV202604090109',
 'distributor', 'Tang Ning', '13600110109', 'Hunan', 'Changsha', 'Kaifu', 'Furong Middle Road 138', 'ambient', 'normal',
 'WH-CS-01', 'Changsha Hub', 'sf', 'SF Express', 622.00, 4, 2,
 'changsha ambient shipment expansion', 'Changsha city order routed to Changsha Hub', 'parcel departed toward Kaifu district station', 'Changsha Kaifu Station',
 '2026-04-09 09:36:00', '2026-04-09 09:44:00', '2026-04-09 10:20:00', '2026-04-09 11:09:00', '2026-04-09 12:18:00',
 '2026-04-09 14:02:00', 'picker-26', 'shipper-26', 'SKU-100861', 'Ambient Gift Box', 'ambient', 1, 268.00,
 268.00, 'SKU-200001', 'Ambient Milk Box', 'ambient', 3, 118.00, 354.00),
(110, 9110, 202604090110, 7020, 'UP202604090110', 'EC202604090110', 'FO202604090110', 'WT202604090110', 'LP202604090110', 'COLD202604090110',
 'retail', 'Xu Man', '13600110110', 'Jiangsu', 'Nanjing', 'Jianye', 'Jiqingmen Street 90', 'cold_chain', 'next_day',
 'WH-NJ-01', 'Nanjing Hub', 'cold', 'Cold Chain Carrier', 590.00, 5, 1,
 'nanjing cold-chain shipment expansion', 'Cold-chain order routed to Nanjing Hub cold room', 'temperature-controlled parcel left hub', 'Nanjing Cold Chain Transit Center',
 '2026-04-09 09:52:00', '2026-04-09 09:58:00', '2026-04-09 10:34:00', '2026-04-09 11:27:00', '2026-04-09 12:36:00',
 '2026-04-09 14:20:00', 'picker-27', 'shipper-27', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 5, 118.00,
 590.00, NULL, NULL, NULL, NULL, NULL, NULL),
(111, 9111, 202604090111, 7021, 'UP202604090111', 'EC202604090111', 'FO202604090111', 'WT202604090111', 'LP202604090111', 'SFV202604090111',
 'ecommerce', 'Luo Jie', '13600110111', 'Hubei', 'Wuhan', 'Qiaokou', 'Jianshe Avenue 108', 'ambient', 'same_day',
 'WH-WH-01', 'Wuhan DC', 'sf', 'SF Express', 660.00, 4, 2,
 'qiaokou same-day shipment expansion', 'Wuhan urban order routed to Wuhan DC', 'parcel heading to Qiaokou distribution station', 'Wuhan Qiaokou Station',
 '2026-04-09 10:10:00', '2026-04-09 10:16:00', '2026-04-09 10:52:00', '2026-04-09 11:40:00', '2026-04-09 12:42:00',
 '2026-04-09 14:26:00', 'picker-28', 'shipper-28', 'SKU-100861', 'Ambient Gift Box', 'ambient', 2, 208.00,
 416.00, 'SKU-200001', 'Ambient Milk Box', 'ambient', 2, 122.00, 244.00),
(112, 9112, 202604090112, 7022, 'UP202604090112', 'EC202604090112', 'FO202604090112', 'WT202604090112', 'LP202604090112', 'COLD202604090112',
 'retail', 'Mo Qi', '13600110112', 'Hunan', 'Changsha', 'Tianxin', 'Shaoshan South Road 218', 'cold_chain', 'next_day',
 'WH-CS-01', 'Changsha Hub', 'cold', 'Cold Chain Carrier', 405.00, 3, 1,
 'tianxin cold-chain shipment expansion', 'Cold-chain order routed to Changsha Hub cold area', 'cold-chain truck departed for Tianxin route', 'Changsha Cold Chain Transit Center',
 '2026-04-09 10:24:00', '2026-04-09 10:30:00', '2026-04-09 11:08:00', '2026-04-09 12:04:00', '2026-04-09 13:10:00',
 '2026-04-09 14:52:00', 'picker-29', 'shipper-29', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 3, 135.00,
 405.00, NULL, NULL, NULL, NULL, NULL, NULL),
(113, 9113, 202604090113, 7023, 'UP202604090113', 'EC202604090113', 'FO202604090113', 'WT202604090113', 'LP202604090113', 'SFV202604090113',
 'ecommerce', 'Han Yi', '13600110113', 'Jiangsu', 'Nanjing', 'Xuanwu', 'Zhongshan East Road 36', 'ambient', 'normal',
 'WH-NJ-01', 'Nanjing Hub', 'sf', 'SF Express', 735.00, 5, 2,
 'xuanwu ambient shipment expansion', 'Nanjing city order routed to Nanjing Hub', 'parcel departed for Xuanwu district station', 'Nanjing Xuanwu Station',
 '2026-04-09 10:40:00', '2026-04-09 10:47:00', '2026-04-09 11:24:00', '2026-04-09 12:18:00', '2026-04-09 13:28:00',
 '2026-04-09 15:06:00', 'picker-30', 'shipper-30', 'SKU-100861', 'Ambient Gift Box', 'ambient', 1, 299.00,
 299.00, 'SKU-200001', 'Ambient Milk Box', 'ambient', 4, 109.00, 436.00),
(114, 9114, 202604090114, 7024, 'UP202604090114', 'EC202604090114', 'FO202604090114', 'WT202604090114', 'LP202604090114', 'SFV202604090114',
 'distributor', 'Pei Chen', '13600110114', 'Hunan', 'Changsha', 'Furong', 'Renmin East Road 98', 'ambient', 'same_day',
 'WH-CS-01', 'Changsha Hub', 'sf', 'SF Express', 695.00, 4, 2,
 'furong same-day shipment expansion', 'Changsha same-day order routed to Changsha Hub', 'parcel left hub for Furong district station', 'Changsha Furong Station',
 '2026-04-09 10:55:00', '2026-04-09 11:03:00', '2026-04-09 11:36:00', '2026-04-09 12:30:00', '2026-04-09 13:42:00',
 '2026-04-09 15:20:00', 'picker-31', 'shipper-31', 'SKU-100861', 'Ambient Gift Box', 'ambient', 3, 182.00,
 546.00, 'SKU-200001', 'Ambient Milk Box', 'ambient', 1, 149.00, 149.00),
(115, 9115, 202604090115, 7025, 'UP202604090115', 'EC202604090115', 'FO202604090115', 'WT202604090115', 'LP202604090115', 'COLD202604090115',
 'retail', 'Yao Bin', '13600110115', 'Hubei', 'Wuhan', 'Qingshan', 'Heping Avenue 156', 'cold_chain', 'next_day',
 'WH-WH-01', 'Wuhan DC', 'cold', 'Cold Chain Carrier', 384.00, 3, 1,
 'qingshan cold-chain shipment expansion', 'Cold-chain order routed to Wuhan DC cold area', 'cold-chain route departed toward Qingshan delivery area', 'Wuhan Cold Chain Transit Center',
 '2026-04-09 11:08:00', '2026-04-09 11:14:00', '2026-04-09 11:48:00', '2026-04-09 12:42:00', '2026-04-09 13:50:00',
 '2026-04-09 15:32:00', 'picker-32', 'shipper-32', 'SKU-300001', 'Cold Chain Yogurt Set', 'cold_chain', 3, 128.00,
 384.00, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO upstream_order (
    id, upstream_order_no, external_no, channel_code, receiver_name, receiver_phone,
    province, city, district, detail_address, temp_layer, requested_delivery,
    total_amount, remark, status, status_text, dispatch_time, fulfillment_order_no,
    target_warehouse_name, create_time, updated_at
)
SELECT
    upstream_id, upstream_order_no, external_no, channel_code, receiver_name, receiver_phone,
    province, city, district, detail_address, temp_layer, requested_delivery,
    total_amount, remark, 'dispatched', 'Dispatched', dispatch_time, order_no,
    warehouse_name, create_time, update_time
FROM demo_shipped_chain_seed;

INSERT INTO upstream_order_item (upstream_order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount)
SELECT upstream_order_no, item1_sku_code, item1_sku_name, item1_temp_layer, item1_qty, item1_unit_price, item1_amount
FROM demo_shipped_chain_seed;

INSERT INTO upstream_order_item (upstream_order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount)
SELECT upstream_order_no, item2_sku_code, item2_sku_name, item2_temp_layer, item2_qty, item2_unit_price, item2_amount
FROM demo_shipped_chain_seed
WHERE item2_sku_code IS NOT NULL;

INSERT INTO oms_order (
    id, order_no, external_no, parent_id, status, status_text, receiver_name, receiver_phone,
    province, city, district, detail_address, warehouse_code, warehouse_name,
    logistics_provider, logistics_provider_name, tracking_number, total_amount,
    route_reason, split_remark, intercept_status, version_no, create_time,
    dispatch_time, outbound_time, update_time
)
SELECT
    oms_id, order_no, external_no, NULL, 50, 'Shipped', receiver_name, receiver_phone,
    province, city, district, detail_address, warehouse_code, warehouse_name,
    logistics_provider, logistics_provider_name, tracking_number, total_amount,
    route_reason, '', 'none', 1, create_time,
    dispatch_time, shipped_at, update_time
FROM demo_shipped_chain_seed;

INSERT INTO oms_order_item (order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount)
SELECT order_no, item1_sku_code, item1_sku_name, item1_temp_layer, item1_qty, item1_unit_price, item1_amount
FROM demo_shipped_chain_seed;

INSERT INTO oms_order_item (order_no, sku_code, sku_name, temp_layer, quantity, unit_price, amount)
SELECT order_no, item2_sku_code, item2_sku_name, item2_temp_layer, item2_qty, item2_unit_price, item2_amount
FROM demo_shipped_chain_seed
WHERE item2_sku_code IS NOT NULL;

INSERT INTO oms_order_log (order_no, log_time, node_code, action_code, operator_name, remark, created_at)
SELECT order_no, create_time, 'OMS', 'created', 'system', 'order created', create_time
FROM demo_shipped_chain_seed
UNION ALL
SELECT order_no, routed_time, 'OMS', 'routed', 'planner', route_reason, routed_time
FROM demo_shipped_chain_seed
UNION ALL
SELECT order_no, dispatch_time, 'OMS', 'dispatched', 'system', 'dispatched to WMS', dispatch_time
FROM demo_shipped_chain_seed
UNION ALL
SELECT order_no, dispatch_time, 'WMS', 'create_task', 'system', CONCAT('wms outbound task created: ', task_no), dispatch_time
FROM demo_shipped_chain_seed
UNION ALL
SELECT order_no, picked_at, 'WMS', 'picked', picker_name, 'task picked', picked_at
FROM demo_shipped_chain_seed
UNION ALL
SELECT order_no, shipped_at, 'WMS', 'shipped', shipper_name, 'task shipped', shipped_at
FROM demo_shipped_chain_seed;

INSERT INTO wms_outbound_task (
    id, task_no, order_no, warehouse_code, warehouse_name, receiver_name, receiver_phone, receiver_address,
    status, status_text, total_qty, total_sku_count, logistics_provider, logistics_provider_name, tracking_number,
    created_at, picked_at, shipped_at, updated_at
)
SELECT
    task_id, task_no, order_no, warehouse_code, warehouse_name, receiver_name, receiver_phone,
    CONCAT(province, ' ', city, ' ', district, ' ', detail_address),
    'shipped', 'Shipped', total_qty, total_sku_count, logistics_provider, logistics_provider_name, tracking_number,
    dispatch_time, picked_at, shipped_at, shipped_at
FROM demo_shipped_chain_seed;

INSERT INTO wms_outbound_task_item (
    task_no, order_no, sku_code, sku_name, quantity, picked_quantity, shipped_quantity, unit_price, created_at, updated_at
)
SELECT task_no, order_no, item1_sku_code, item1_sku_name, item1_qty, item1_qty, item1_qty, item1_unit_price, dispatch_time, shipped_at
FROM demo_shipped_chain_seed;

INSERT INTO wms_outbound_task_item (
    task_no, order_no, sku_code, sku_name, quantity, picked_quantity, shipped_quantity, unit_price, created_at, updated_at
)
SELECT task_no, order_no, item2_sku_code, item2_sku_name, item2_qty, item2_qty, item2_qty, item2_unit_price, dispatch_time, shipped_at
FROM demo_shipped_chain_seed
WHERE item2_sku_code IS NOT NULL;

INSERT INTO wms_outbound_task_log (task_no, log_time, action_code, operator_name, remark, created_at)
SELECT task_no, dispatch_time, 'create', 'system', 'outbound task created', dispatch_time
FROM demo_shipped_chain_seed
UNION ALL
SELECT task_no, picked_at, 'pick', picker_name, 'task picked', picked_at
FROM demo_shipped_chain_seed
UNION ALL
SELECT task_no, shipped_at, 'ship', shipper_name, 'task shipped', shipped_at
FROM demo_shipped_chain_seed;

INSERT INTO lgs_parcel (
    parcel_no, order_no, provider_code, provider_name, tracking_number, status, status_text,
    receiver_name, receiver_phone, receiver_address, signed_by, signed_at, delivery_remark, created_at, updated_at
)
SELECT
    parcel_no, order_no, logistics_provider, logistics_provider_name, tracking_number, 'in_transit', 'In Transit',
    receiver_name, receiver_phone, CONCAT(province, ' ', city, ' ', district, ' ', detail_address),
    NULL, NULL, delivery_remark, shipped_at, update_time
FROM demo_shipped_chain_seed;

INSERT INTO lgs_parcel_track (
    parcel_no, event_code, event_name, event_time, location, operator_name, remark, created_at
)
SELECT parcel_no, 'created', 'Parcel Created', shipped_at, warehouse_name, shipper_name, 'handover to carrier', shipped_at
FROM demo_shipped_chain_seed
UNION ALL
SELECT parcel_no, 'in_transit', 'In Transit', update_time, transit_location, 'route-bot',
       CASE
           WHEN logistics_provider = 'cold' THEN 'temperature-controlled vehicle departed'
           ELSE 'vehicle departed for local station'
       END,
       update_time
FROM demo_shipped_chain_seed;

INSERT INTO lgs_callback_record (
    order_no, provider_code, callback_type, callback_status, request_payload, result_payload, created_at, updated_at
)
SELECT
    order_no,
    logistics_provider,
    'track_sync',
    'success',
    CONCAT('{"trackingNumber":"', tracking_number, '","orderNo":"', order_no, '"}'),
    CASE
        WHEN logistics_provider = 'cold' THEN '{"status":"in_transit","temperature":"2-6C"}'
        ELSE CONCAT('{"status":"in_transit","location":"', transit_location, '"}')
    END,
    update_time,
    update_time
FROM demo_shipped_chain_seed;

DROP TEMPORARY TABLE demo_shipped_chain_seed;

COMMIT;
