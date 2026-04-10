package com.novadepot.backend.modules.warehouses;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.WarehouseEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehousesController {
    private final WarehousesService service;

    public WarehousesController(WarehousesService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("WAREHOUSE_READ")
    public ApiResponse<List<WarehouseEntity>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("WAREHOUSE_READ")
    public ApiResponse<WarehouseEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("WAREHOUSE_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody WarehouseCreateRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }
}
