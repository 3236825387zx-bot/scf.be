package com.scf.wms.interfaces.controller;

import com.scf.common.result.Result;
import com.scf.wms.application.service.WmsOutboundService;
import com.scf.wms.interfaces.dto.OutboundCreateRequest;
import com.scf.wms.interfaces.dto.TaskActionRequest;
import com.scf.wms.interfaces.dto.TaskShipRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/wms/outbound")
public class WmsOutboundController {

    private final WmsOutboundService wmsOutboundService;

    public WmsOutboundController(WmsOutboundService wmsOutboundService) {
        this.wmsOutboundService = wmsOutboundService;
    }

    @PostMapping("/create")
    public Boolean createOutboundTask(@RequestBody OutboundCreateRequest request) {
        return wmsOutboundService.createOutboundTask(request);
    }

    @GetMapping("/tasks")
    public Result<Map<String, Object>> listTasks(@RequestParam Map<String, String> query) {
        return Result.ok(wmsOutboundService.listTasks(query));
    }

    @GetMapping("/tasks/{taskNo}")
    public Result<Map<String, Object>> getTask(@PathVariable("taskNo") String taskNo) {
        return Result.ok(wmsOutboundService.getTask(taskNo));
    }

    @GetMapping("/tasks/by-order/{orderNo}")
    public Result<Map<String, Object>> getTaskByOrderNo(@PathVariable("orderNo") String orderNo) {
        return Result.ok(wmsOutboundService.getTaskByOrderNo(orderNo));
    }

    @PostMapping("/tasks/{taskNo}/pick")
    public Result<Map<String, Object>> pickTask(@PathVariable("taskNo") String taskNo,
                                                @RequestBody(required = false) TaskActionRequest request) {
        return Result.ok(wmsOutboundService.pickTask(taskNo, request));
    }

    @PostMapping("/tasks/{taskNo}/ship")
    public Result<Map<String, Object>> shipTask(@PathVariable("taskNo") String taskNo,
                                                @RequestBody TaskShipRequest request) {
        return Result.ok(wmsOutboundService.shipTask(taskNo, request));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(wmsOutboundService.dashboard());
    }
}
