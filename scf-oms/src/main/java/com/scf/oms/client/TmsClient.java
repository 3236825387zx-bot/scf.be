package com.scf.oms.client;

import com.scf.oms.interfaces.dto.RouteEstimateDTO;
import com.scf.oms.interfaces.dto.RouteReq;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 运输管理系统 (TMS) 客户端
 * 负责物流时效和运费的预估调用
 */
@Deprecated
public interface TmsClient {

    /**
     * 路由预估 (时效和运费)
     *
     * @param req 路由请求，包含起始/目的地信息和货品信息等
     * @return 返回可能的路由预估结果列表（包含预计时效、费用等）
     */
    @PostMapping("/api/lgs/route/estimate")
    List<RouteEstimateDTO> estimateRoute(@RequestBody RouteReq req);
}
