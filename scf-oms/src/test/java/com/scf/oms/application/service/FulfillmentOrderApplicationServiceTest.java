package com.scf.oms.application.service;

import com.scf.common.exception.BusinessException;
import com.scf.oms.client.IscClient;
import com.scf.oms.client.WmsClient;
import com.scf.oms.application.enums.ErrorCode;
import com.scf.oms.domain.model.FulfillmentOrder;
import com.scf.oms.domain.repository.FulfillmentOrderRepository;
import com.scf.oms.domain.service.RoutingDecisionDomainService;
import com.scf.oms.infrastructure.repository.InMemoryFulfillmentOrderRepository;
import com.scf.oms.interfaces.dto.OrderCreateReq;
import com.scf.oms.interfaces.dto.StockLockReq;
import com.scf.oms.interfaces.dto.StockUnlockReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class FulfillmentOrderApplicationServiceTest {

    private FulfillmentOrderService service;
    private FulfillmentOrderRepository repository;
    private RoutingDecisionDomainService routingService;
    private IscClient iscClient;
    private WmsClient wmsClient;

    @BeforeEach
    void setup() {
        repository = new InMemoryFulfillmentOrderRepository();
        routingService = Mockito.mock(RoutingDecisionDomainService.class);
        iscClient = Mockito.mock(IscClient.class);
        wmsClient = Mockito.mock(WmsClient.class);
        service = new com.scf.oms.application.service.impl.FulfillmentOrderServiceImpl(repository, routingService, iscClient, wmsClient);
    }

    @Test
    void testCreateOrderHappyPath() {
        when(routingService.calculateOptimalWarehouse(any())).thenReturn("WH1");
        when(iscClient.lockStock(any(StockLockReq.class))).thenReturn(true);
        when(wmsClient.createOutboundTask(any())).thenReturn(true);

        OrderCreateReq req = new OrderCreateReq();
        req.setExternalOrderId("EXT123");
        OrderCreateReq.SkuItem sku = new OrderCreateReq.SkuItem();
        sku.setSkuCode("SKU1");
        sku.setSkuName("Name");
        sku.setQuantity(1);
        sku.setPrice(BigDecimal.valueOf(10));
        req.setSkuList(Collections.singletonList(sku));
        req.setReceiverName("Zhang San");
        req.setReceiverPhone("13800138000");
        req.setProvince("Province");
        req.setCity("City");
        req.setDistrict("District");
        req.setDetailAddress("Address");

        String orderId = service.createOrder(req);
        assertNotNull(orderId);

        FulfillmentOrder saved = repository.findById(orderId).orElse(null);
        assertNotNull(saved);
        assertEquals("WH1", saved.getWarehouseId());
    }

    @Test
    void testCreateOrderRejectsEmptySkuList() {
        OrderCreateReq req = new OrderCreateReq();
        req.setExternalOrderId("EXT-EMPTY");
        req.setReceiverName("Zhang San");
        req.setReceiverPhone("13800138000");
        req.setProvince("Province");
        req.setCity("City");
        req.setDistrict("District");
        req.setDetailAddress("Address");
        req.setSkuList(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(req));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verify(routingService, never()).calculateOptimalWarehouse(any());
    }

    @Test
    void testCreateOrderUnlocksStockWhenWmsDispatchFails() {
        when(routingService.calculateOptimalWarehouse(any())).thenReturn("WH1");
        when(iscClient.lockStock(any(StockLockReq.class))).thenReturn(true);
        when(wmsClient.createOutboundTask(any())).thenReturn(false);

        OrderCreateReq req = new OrderCreateReq();
        req.setExternalOrderId("EXT-WMS-FAIL");
        OrderCreateReq.SkuItem sku = new OrderCreateReq.SkuItem();
        sku.setSkuCode("SKU1");
        sku.setSkuName("Name");
        sku.setQuantity(1);
        sku.setPrice(BigDecimal.valueOf(10));
        req.setSkuList(Collections.singletonList(sku));
        req.setReceiverName("Zhang San");
        req.setReceiverPhone("13800138000");
        req.setProvince("Province");
        req.setCity("City");
        req.setDistrict("District");
        req.setDetailAddress("Address");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(req));
        assertEquals(ErrorCode.WMS_DISPATCH_FAILED, ex.getErrorCode());
        verify(iscClient).unlockStock(any(StockUnlockReq.class));
    }
}
