package com.scf.isc.controller;

import com.scf.isc.application.service.IscInventoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(originPatterns = "*")
@RequestMapping("/api/isc/stock")
public class IscStockController {

    private final IscInventoryService iscInventoryService;

    public IscStockController(IscInventoryService iscInventoryService) {
        this.iscInventoryService = iscInventoryService;
    }

    @PostMapping("/queryAtp")
    public List<Map<String, Object>> queryAtp(@RequestBody Map<String, Object> req) {
        return iscInventoryService.queryAtp(req);
    }

    @PostMapping("/lock")
    @Transactional
    public Boolean lockStock(@RequestBody Map<String, Object> req) {
        return iscInventoryService.lockStock(req);
    }

    @PostMapping("/unlock")
    @Transactional
    public Boolean unlockStock(@RequestBody Map<String, Object> req) {
        return iscInventoryService.unlockStock(req);
    }
}
