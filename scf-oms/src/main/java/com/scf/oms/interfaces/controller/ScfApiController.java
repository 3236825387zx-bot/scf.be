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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scf")
@RequiredArgsConstructor
public class ScfApiController {

    private final ScfFacadeService scfFacadeService;

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.login(req));
    }

    @PostMapping("/auth/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.register(req));
    }

    @GetMapping("/users/me")
    public Result<Map<String, Object>> currentUser() {
        return Result.ok(scfFacadeService.currentUser());
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users() {
        return Result.ok(scfFacadeService.users());
    }

    @GetMapping("/navigation")
    public Result<Map<String, Object>> navigation() {
        return Result.ok(scfFacadeService.navigation());
    }

    @GetMapping("/upstream-orders")
    public Result<Map<String, Object>> upstreamOrders(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.upstreamOrders(query));
    }

    @PostMapping("/upstream-orders/{id}/dispatch")
    public Result<Map<String, Object>> dispatchUpstreamOrder(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.dispatchUpstreamOrder(id));
    }

    @GetMapping("/oms/orders")
    public Result<Map<String, Object>> omsOrders(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.omsOrders(query));
    }

    @GetMapping("/oms/workspace")
    public Result<Map<String, Object>> omsWorkspace(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.omsWorkspace(query));
    }

    @GetMapping("/oms/orders/{id}")
    public Result<Map<String, Object>> omsOrderDetail(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.omsOrderDetail(id));
    }

    @GetMapping("/oms/rules")
    public Result<Map<String, Object>> omsRules(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.omsRules(query));
    }

    @PostMapping("/oms/rules")
    public Result<Map<String, Object>> createOmsRule(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.createOmsRule(req));
    }

    @PutMapping("/oms/rules/{id}")
    public Result<Map<String, Object>> updateOmsRule(@PathVariable("id") Long id, @RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.updateOmsRule(id, req));
    }

    @DeleteMapping("/oms/rules/{id}")
    public Result<Boolean> deleteOmsRule(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.deleteOmsRule(id));
    }

    @GetMapping({"/oms/split-merge", "/oms/split-merge/requests"})
    public Result<Map<String, Object>> splitMergeRequests(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.splitMergeRequests(query));
    }

    @PostMapping({"/oms/split-merge", "/oms/split-merge/requests"})
    public Result<Map<String, Object>> createSplitMergeRequest(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.createSplitMergeRequest(req));
    }

    @PostMapping({"/oms/split-merge/{id}/execute", "/oms/split-merge/requests/{id}/execute"})
    public Result<Map<String, Object>> executeSplitMergeRequest(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.executeSplitMergeRequest(id));
    }

    @PostMapping({"/oms/split-merge/{id}/cancel", "/oms/split-merge/requests/{id}/cancel"})
    public Result<Map<String, Object>> cancelSplitMergeRequest(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.cancelSplitMergeRequest(id));
    }

    @GetMapping("/oms/dashboard")
    public Result<Map<String, Object>> omsDashboard() {
        return Result.ok(scfFacadeService.omsDashboard());
    }

    @GetMapping("/oms/exceptions")
    public Result<Map<String, Object>> omsExceptions(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.omsExceptions(query));
    }

    @GetMapping("/oms/exceptions/{id}")
    public Result<Map<String, Object>> omsExceptionDetail(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.omsExceptionDetail(id));
    }

    @PostMapping("/oms/exceptions/{id}/release-inventory")
    public Result<Boolean> releaseOmsExceptionInventory(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.releaseOmsExceptionInventory(id));
    }

    @PostMapping("/oms/exceptions/{id}/rewrite-status")
    public Result<Boolean> rewriteOmsExceptionStatus(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.rewriteOmsExceptionStatus(id));
    }

    @PostMapping("/oms/exceptions/{id}/compensation-log")
    public Result<Map<String, Object>> generateOmsExceptionCompensationLog(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.generateOmsExceptionCompensationLog(id));
    }

    @PostMapping("/oms/simulation/dispatch")
    public Result<Map<String, Object>> simulateOmsDispatch(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.simulateOmsDispatch(req));
    }

    @GetMapping("/lgs/providers")
    public Result<Map<String, Object>> lgsProviders(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.lgsProviders(query));
    }

    @PostMapping("/lgs/providers")
    public Result<Map<String, Object>> saveLgsProvider(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.saveLgsProvider(req));
    }

    @PostMapping("/lgs/providers/{code}/toggle-status")
    public Result<Map<String, Object>> toggleLgsProviderStatus(@PathVariable("code") String code) {
        return Result.ok(scfFacadeService.toggleLgsProviderStatus(code));
    }

    @GetMapping({"/lgs/delivery", "/lgs/deliveries"})
    public Result<Map<String, Object>> lgsDeliveries(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.lgsDeliveries(query));
    }

    @GetMapping({"/lgs/callback", "/lgs/callbacks"})
    public Result<Map<String, Object>> lgsCallbacks(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.lgsCallbacks(query));
    }

    @PostMapping({"/lgs/intercepts", "/lgs/callbacks/intercept"})
    public Result<Map<String, Object>> submitLgsIntercept(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.submitLgsIntercept(req));
    }

    @GetMapping("/isc/ledger")
    public Result<Map<String, Object>> iscLedger(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.iscLedger(query));
    }

    @GetMapping({"/isc/adjustments", "/isc/adjustment"})
    public Result<Map<String, Object>> iscAdjustment(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.iscAdjustment(query));
    }

    @GetMapping("/isc/alerts")
    public Result<Map<String, Object>> iscAlerts(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.iscAlerts(query));
    }

    @GetMapping("/wms/taskhall")
    public Result<Map<String, Object>> wmsTaskHall(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.wmsTaskHall(query));
    }

    @GetMapping("/wms/taskhall/{waveId}")
    public Result<Map<String, Object>> wmsWaveDetail(@PathVariable("waveId") String waveId) {
        return Result.ok(scfFacadeService.wmsWaveDetail(waveId));
    }

    @PostMapping("/wms/taskhall")
    public Result<Map<String, Object>> createWmsWave(@RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.createWmsWave(req));
    }

    @PutMapping("/wms/taskhall/{waveId}")
    public Result<Map<String, Object>> updateWmsWave(@PathVariable("waveId") String waveId,
                                                     @RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.updateWmsWave(waveId, req));
    }

    @DeleteMapping("/wms/taskhall/{waveId}")
    public Result<Boolean> deleteWmsWave(@PathVariable("waveId") String waveId) {
        return Result.ok(scfFacadeService.deleteWmsWave(waveId));
    }

    @GetMapping("/wms/picking")
    public Result<Map<String, Object>> wmsPicking(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.wmsPicking(query));
    }

    @GetMapping("/wms/picking/{id}")
    public Result<Map<String, Object>> wmsPickingDetail(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.wmsPickingDetail(id));
    }

    @PutMapping("/wms/picking/{id}")
    public Result<Map<String, Object>> updateWmsPickingTask(@PathVariable("id") Long id,
                                                            @RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.updateWmsPickingTask(id, req));
    }

    @GetMapping("/wms/packing")
    public Result<Map<String, Object>> wmsPacking(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.wmsPacking(query));
    }

    @GetMapping("/wms/packing/{id}")
    public Result<Map<String, Object>> wmsPackingDetail(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.wmsPackingDetail(id));
    }

    @PutMapping("/wms/packing/{id}")
    public Result<Map<String, Object>> updateWmsPackingOrder(@PathVariable("id") Long id,
                                                             @RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.updateWmsPackingOrder(id, req));
    }

    @GetMapping("/wms/shipment")
    public Result<Map<String, Object>> wmsShipment(@RequestParam Map<String, String> query) {
        return Result.ok(scfFacadeService.wmsShipment(query));
    }

    @GetMapping("/wms/shipment/{id}")
    public Result<Map<String, Object>> wmsShipmentDetail(@PathVariable("id") Long id) {
        return Result.ok(scfFacadeService.wmsShipmentDetail(id));
    }

    @PutMapping("/wms/shipment/{id}")
    public Result<Map<String, Object>> updateWmsShipmentRecord(@PathVariable("id") Long id,
                                                               @RequestBody Map<String, Object> req) {
        return Result.ok(scfFacadeService.updateWmsShipmentRecord(id, req));
    }

    @GetMapping("/dashboard/overview")
    public Result<Map<String, Object>> dashboardOverview() {
        return Result.ok(scfFacadeService.dashboardOverview());
    }
}
