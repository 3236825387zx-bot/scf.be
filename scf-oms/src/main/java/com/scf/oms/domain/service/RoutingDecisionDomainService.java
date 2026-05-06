package com.scf.oms.domain.service;

import com.scf.oms.client.IscClient;
import com.scf.oms.domain.model.FulfillmentOrder;
import com.scf.oms.domain.model.FulfillmentOrderDetail;
import com.scf.oms.interfaces.dto.SkuStockDTO;
import com.scf.oms.interfaces.dto.StockQueryReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingDecisionDomainService {

    private final IscClient iscClient;

    public String calculateOptimalWarehouse(FulfillmentOrder order) {
        log.info("Calculating warehouse for order {}", order.getOrderId());

        StockQueryReq queryReq = new StockQueryReq();
        queryReq.setSkuList(order.getDetails().stream()
                .map(FulfillmentOrderDetail::getSkuId)
                .distinct()
                .toList());

        List<SkuStockDTO> stockList = iscClient.queryAtp(queryReq);
        if (stockList == null || stockList.isEmpty()) {
            log.warn("No ATP stock found for order {}", order.getOrderId());
            return null;
        }

        Map<String, List<SkuStockDTO>> warehouseStocks = stockList.stream()
                .filter(stock -> stock.getWarehouseId() != null)
                .collect(Collectors.groupingBy(SkuStockDTO::getWarehouseId));

        for (Map.Entry<String, List<SkuStockDTO>> entry : warehouseStocks.entrySet()) {
            boolean matched = order.getDetails().stream().allMatch(detail ->
                    entry.getValue().stream().anyMatch(stock ->
                            Objects.equals(stock.getSkuCode(), detail.getSkuId())
                                    && stock.getAvailableQuantity() >= detail.getQuantity()));
            if (matched) {
                log.info("Selected warehouse {} for order {}", entry.getKey(), order.getOrderId());
                return entry.getKey();
            }
        }

        log.warn("No warehouse can fulfill order {}", order.getOrderId());
        return null;
    }
}
