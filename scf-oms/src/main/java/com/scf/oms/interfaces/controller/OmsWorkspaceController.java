package com.scf.oms.interfaces.controller;

import com.scf.common.result.Result;
import com.scf.oms.application.service.ScfFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/oms")
@RequiredArgsConstructor
public class OmsWorkspaceController {

    private final ScfFacadeService scfFacadeService;

    @GetMapping("/upstream-orders")
    public Result<Map<String, Object>> upstreamOrders(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.upstreamOrders(query));
    }

    @PostMapping("/upstream-orders/{id}/dispatch")
    public Result<Map<String, Object>> dispatchUpstreamOrder(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.dispatchUpstreamOrder(id));
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> omsOrders(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.omsOrders(query));
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> omsOrderDetail(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.omsOrderDetail(id));
    }

    @GetMapping("/rules")
    public Result<Map<String, Object>> omsRules(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.omsRules(query));
    }

    @PostMapping("/rules")
    public Result<Map<String, Object>> createOmsRule(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.createOmsRule(req));
    }

    @PutMapping("/rules/{id}")
    public Result<Map<String, Object>> updateOmsRule(@PathVariable("id") Long id, @RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.updateOmsRule(id, req));
    }

    @DeleteMapping("/rules/{id}")
    public Result<Boolean> deleteOmsRule(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.deleteOmsRule(id));
    }

    @GetMapping("/split-merge/requests")
    public Result<Map<String, Object>> splitMergeRequests(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.splitMergeRequests(query));
    }

    @PostMapping("/split-merge/requests")
    public Result<Map<String, Object>> createSplitMergeRequest(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.createSplitMergeRequest(req));
    }

    @PostMapping("/split-merge/requests/{id}/execute")
    public Result<Map<String, Object>> executeSplitMergeRequest(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.executeSplitMergeRequest(id));
    }

    @PostMapping("/split-merge/requests/{id}/cancel")
    public Result<Map<String, Object>> cancelSplitMergeRequest(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.cancelSplitMergeRequest(id));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> omsDashboard() {
        return Result.ok(scfFacadeService.omsDashboard());
    }
}
