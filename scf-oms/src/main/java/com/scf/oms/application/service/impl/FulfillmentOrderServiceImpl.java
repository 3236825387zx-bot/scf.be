package com.scf.oms.application.service.impl;

import com.scf.common.exception.BusinessException;
import com.scf.oms.application.enums.ErrorCode;
import com.scf.oms.application.service.FulfillmentOrderService;
import com.scf.oms.client.IscClient;
import com.scf.oms.client.WmsClient;
import com.scf.oms.domain.model.FulfillmentOrder;
import com.scf.oms.domain.model.ReceiverInfo;
import com.scf.oms.domain.repository.FulfillmentOrderRepository;
import com.scf.oms.domain.service.RoutingDecisionDomainService;
import com.scf.oms.interfaces.dto.OrderCreateReq;
import com.scf.oms.interfaces.dto.OutboundTaskReq;
import com.scf.oms.interfaces.dto.StockLockReq;
import com.scf.oms.interfaces.dto.StockUnlockReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FulfillmentOrderServiceImpl implements FulfillmentOrderService {

    private final FulfillmentOrderRepository orderRepository;
    private final RoutingDecisionDomainService routingService;
    private final IscClient iscClient;
    private final WmsClient wmsClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderCreateReq req) {
        validateCreateOrderRequest(req);
        log.info("Creating fulfillment order for externalOrderId={}", req.getExternalOrderId());

        Optional<FulfillmentOrder> existingOrder = orderRepository.findByExternalId(req.getExternalOrderId());
        if (existingOrder.isPresent()) {
            return existingOrder.get().getOrderId();
        }

        String orderId = "FO" + System.currentTimeMillis();
        ReceiverInfo receiverInfo = new ReceiverInfo(
                req.getReceiverName(),
                req.getReceiverPhone(),
                req.getProvince(),
                req.getCity(),
                req.getDistrict(),
                req.getDetailAddress()
        );

        FulfillmentOrder order = new FulfillmentOrder(orderId, req.getExternalOrderId(), receiverInfo);
        if (req.getSkuList() != null) {
            req.getSkuList().forEach(sku ->
                    order.addDetail(sku.getSkuCode(), sku.getSkuName(), sku.getQuantity(), sku.getPrice()));
        }

        String warehouseId = routingService.calculateOptimalWarehouse(order);
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new BusinessException(
                    ErrorCode.NO_AVAILABLE_WAREHOUSE,
                    null,
                    "orderId=" + orderId + ", externalOrderId=" + req.getExternalOrderId()
            );
        }
        order.assignWarehouse(warehouseId);

        StockLockReq lockReq = new StockLockReq();
        lockReq.setOrderId(orderId);
        lockReq.setWarehouseId(warehouseId);
        lockReq.setSkuList(order.getDetails().stream().map(detail -> {
            StockLockReq.LockItem item = new StockLockReq.LockItem();
            item.setSkuCode(detail.getSkuId());
            item.setQuantity(detail.getQuantity());
            return item;
        }).collect(Collectors.toList()));

        if (!Boolean.TRUE.equals(iscClient.lockStock(lockReq))) {
            throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED, null, lockReq);
        }
        order.stockLocked();

        OutboundTaskReq taskReq = new OutboundTaskReq();
        taskReq.setOrderId(orderId);
        taskReq.setWarehouseId(warehouseId);
        OutboundTaskReq.ReceiverInfo taskReceiver = new OutboundTaskReq.ReceiverInfo();
        taskReceiver.setName(receiverInfo.getName());
        taskReceiver.setPhone(receiverInfo.getPhone());
        taskReceiver.setAddress(receiverInfo.getDetailAddress());
        taskReq.setReceiverInfo(taskReceiver);
        taskReq.setItems(order.getDetails().stream().map(detail -> {
            OutboundTaskReq.OutboundItem item = new OutboundTaskReq.OutboundItem();
            item.setSkuCode(detail.getSkuId());
            item.setQuantity(detail.getQuantity());
            return item;
        }).collect(Collectors.toList()));

        if (!Boolean.TRUE.equals(wmsClient.createOutboundTask(taskReq))) {
            releaseLockedStock(orderId, warehouseId, lockReq.getSkuList(), "wms dispatch failed");
            throw new BusinessException(ErrorCode.WMS_DISPATCH_FAILED, null, taskReq);
        }
        order.dispatched();

        orderRepository.save(order);
        log.info("Created fulfillment order {}", orderId);
        return orderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(String orderId, String reason) {
        FulfillmentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order not found: " + orderId, orderId));

        order.cancel();

        try {
            StockUnlockReq unlockReq = new StockUnlockReq();
            unlockReq.setOrderId(orderId);
            unlockReq.setReason(reason);
            iscClient.unlockStock(unlockReq);
        } catch (Exception ex) {
            log.warn("Failed to unlock stock for order {}", orderId, ex);
        }

        orderRepository.save(order);
        return true;
    }

    @Override
    public Optional<FulfillmentOrder> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<FulfillmentOrder> getOrders() {
        List<FulfillmentOrder> orders = orderRepository.findAll();
        return orders == null ? Collections.emptyList() : orders;
    }

    private void validateCreateOrderRequest(OrderCreateReq req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "request body is required");
        }
        if (req.getExternalOrderId() == null || req.getExternalOrderId().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "externalOrderId is required");
        }
        if (req.getSkuList() == null || req.getSkuList().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "skuList must not be empty");
        }
    }

    private void releaseLockedStock(String orderId, String warehouseId, List<StockLockReq.LockItem> lockItems, String reason) {
        try {
            StockUnlockReq unlockReq = new StockUnlockReq();
            unlockReq.setOrderId(orderId);
            unlockReq.setReason(reason);
            unlockReq.setWarehouseId(warehouseId);
            unlockReq.setSkuList(lockItems == null ? Collections.emptyList() : lockItems.stream().map(item -> {
                StockUnlockReq.UnlockItem unlockItem = new StockUnlockReq.UnlockItem();
                unlockItem.setSkuCode(item.getSkuCode());
                unlockItem.setQuantity(item.getQuantity());
                return unlockItem;
            }).collect(Collectors.toList()));
            iscClient.unlockStock(unlockReq);
        } catch (Exception ex) {
            log.warn("Failed to release locked stock for order {}", orderId, ex);
        }
    }
}
