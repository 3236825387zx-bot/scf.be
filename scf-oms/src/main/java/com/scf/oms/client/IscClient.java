package com.scf.oms.client;

import com.scf.oms.interfaces.dto.SkuStockDTO;
import com.scf.oms.interfaces.dto.StockLockReq;
import com.scf.oms.interfaces.dto.StockQueryReq;
import com.scf.oms.interfaces.dto.StockUnlockReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 库存中心 (ISC) 客户端
 * 负责库存查询（ATP）、库存锁定与释放操作的远程调用。
 * 注意：FeignClient 的超时与重试策略应在客户端配置或网关层配置中设置。
 */
@FeignClient(name = "scf-isc", url = "${scf.isc.url:http://localhost:8081}")
public interface IscClient {

    /**
     * 查询可用库存 (ATP)
     *
     * @param req 库存查询请求，包含需要查询的 SKU 列表及可选的仓库/区域过滤
     * @return 返回 SKU 在各仓库的可用库存列表，包含 warehouseId、skuCode、availableQuantity 等信息
     */
    @PostMapping("/api/isc/stock/queryAtp")
    List<SkuStockDTO> queryAtp(@RequestBody StockQueryReq req);

    /**
     * 锁定库存
     *
     * @param req 库存锁定请求，包含 orderId、warehouseId 及待锁定的 SKU 列表
     * @return 返回 true 表示锁定成功，false 表示锁定失败（例如库存不足）
     */
    @PostMapping("/api/isc/stock/lock")
    Boolean lockStock(@RequestBody StockLockReq req);

    /**
     * 释放/解锁库存
     *
     * @param req 库存解锁请求，包含 orderId 及解锁原因
     * @return 返回 true 表示解锁成功
     */
    @PostMapping("/api/isc/stock/unlock")
    Boolean unlockStock(@RequestBody StockUnlockReq req);
}
