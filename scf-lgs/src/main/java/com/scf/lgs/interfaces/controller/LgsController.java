package com.scf.lgs.interfaces.controller;

import com.scf.common.result.Result;
import com.scf.lgs.application.service.LgsService;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(originPatterns = "*")
@RequestMapping("/api/lgs")
public class LgsController {

    private final LgsService lgsService;

    public LgsController(LgsService lgsService) {
        this.lgsService = lgsService;
    }

    @PostMapping("/route/estimate")
    public List<Map<String, Object>> estimateRoute(@RequestBody Map<String, Object> request) {
        return lgsService.estimateRoute(request);
    }

    @GetMapping("/providers")
    public Result<Map<String, Object>> listProviders(@RequestParam Map<String, String> query) {
        return Result.ok(lgsService.listProviders(query));
    }

    @GetMapping("/providers/{providerCode}")
    public Result<Map<String, Object>> getProvider(@PathVariable("providerCode") String providerCode) {
        return Result.ok(lgsService.getProvider(providerCode));
    }

    @PostMapping("/providers")
    public Result<Map<String, Object>> createProvider(@RequestBody Map<String, Object> request) {
        return Result.ok(lgsService.createProvider(request));
    }

    @PutMapping("/providers/{providerCode}")
    public Result<Map<String, Object>> updateProvider(@PathVariable("providerCode") String providerCode,
                                                      @RequestBody Map<String, Object> request) {
        return Result.ok(lgsService.updateProvider(providerCode, request));
    }

    @PostMapping("/providers/{providerCode}/enable")
    public Result<Map<String, Object>> enableProvider(@PathVariable("providerCode") String providerCode) {
        return Result.ok(lgsService.updateProviderStatus(providerCode, true));
    }

    @PostMapping("/providers/{providerCode}/disable")
    public Result<Map<String, Object>> disableProvider(@PathVariable("providerCode") String providerCode) {
        return Result.ok(lgsService.updateProviderStatus(providerCode, false));
    }

    @GetMapping("/parcels")
    public Result<Map<String, Object>> listParcels(@RequestParam Map<String, String> query) {
        return Result.ok(lgsService.listParcels(query));
    }

    @GetMapping("/parcels/{parcelNo}")
    public Result<Map<String, Object>> getParcel(@PathVariable("parcelNo") String parcelNo) {
        return Result.ok(lgsService.getParcel(parcelNo));
    }

    @PostMapping("/parcels/deliver")
    public Result<Map<String, Object>> deliverParcel(@RequestBody Map<String, Object> request) {
        return Result.ok(lgsService.deliverParcel(request));
    }

    @PostMapping("/parcels/{parcelNo}/sign")
    public Result<Map<String, Object>> signParcel(@PathVariable("parcelNo") String parcelNo,
                                                  @RequestBody(required = false) Map<String, Object> request) {
        return Result.ok(lgsService.signParcel(parcelNo, request == null ? Map.of() : request));
    }

    @PostMapping("/parcels/{parcelNo}/tracking")
    public Result<Map<String, Object>> appendTracking(@PathVariable("parcelNo") String parcelNo,
                                                      @RequestBody Map<String, Object> request) {
        return Result.ok(lgsService.appendTracking(parcelNo, request));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(lgsService.dashboard());
    }
}
