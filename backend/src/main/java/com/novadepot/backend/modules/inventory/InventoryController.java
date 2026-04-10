package com.novadepot.backend.modules.inventory;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("INVENTORY_READ")
    public ApiResponse<List<InventoryEntity>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @GetMapping("/transactions")
    @RequirePermission("INVENTORY_TXN_READ")
    public ApiResponse<List<InventoryTransactionEntity>> transactions() {
        return ApiResponse.success(service.transactions(), MDC.get("traceId"));
    }

    @GetMapping("/alerts/low-stock")
    @RequirePermission("INVENTORY_ALERT_READ")
    public ApiResponse<List<InventoryEntity>> lowStockAlerts() {
        return ApiResponse.success(service.lowStockAlerts(), MDC.get("traceId"));
    }
}
