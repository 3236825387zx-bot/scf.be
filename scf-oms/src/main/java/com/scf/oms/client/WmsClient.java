package com.scf.oms.client;

import com.scf.oms.interfaces.dto.OutboundTaskReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 仓储管理系统 (WMS) 客户端
 * 负责将出库任务下发给 WMS，并接收 WMS 的创建结果。
 */
@FeignClient(name = "scf-wms", url = "${scf.wms.url:http://localhost:8082}")
public interface WmsClient {

    /**
     * 创建出库任务
     *
     * @param req 出库任务请求，包含 orderId、warehouseId、收货人信息与出库明细
     * @return 返回 true 表示 WMS 已成功接收并创建出库任务
     */
    @PostMapping("/api/wms/outbound/create")
    Boolean createOutboundTask(@RequestBody OutboundTaskReq req);
}
