package com.scf.oms.client;

import com.scf.oms.interfaces.dto.RouteEstimateDTO;
import com.scf.oms.interfaces.dto.RouteReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "scf-lgs", url = "${scf.lgs.url:http://localhost:8083}")
public interface LgsClient {

    @PostMapping("/api/lgs/route/estimate")
    List<RouteEstimateDTO> estimateRoute(@RequestBody RouteReq req);
}
