package com.scf.isc.controller;

import com.scf.common.result.Result;
import com.scf.isc.application.service.IscInventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/isc")
public class IscWorkspaceController {

    private final IscInventoryService iscInventoryService;

    public IscWorkspaceController(IscInventoryService iscInventoryService) {
        this.iscInventoryService = iscInventoryService;
    }

    @GetMapping("/ledger")
    public Result<Map<String, Object>> ledger() {
        return Result.ok(iscInventoryService.ledger());
    }

    @GetMapping({"/adjustments", "/adjustment"})
    public Result<Map<String, Object>> adjustment() {
        return Result.ok(iscInventoryService.adjustment());
    }

    @GetMapping("/alerts")
    public Result<Map<String, Object>> alerts() {
        return Result.ok(iscInventoryService.alerts());
    }
}
