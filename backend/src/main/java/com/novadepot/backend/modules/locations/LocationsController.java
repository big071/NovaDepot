package com.novadepot.backend.modules.locations;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.WarehouseLocationEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationsController {
    private final LocationsService service;

    public LocationsController(LocationsService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("LOCATION_READ")
    public ApiResponse<List<WarehouseLocationEntity>> list(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.success(service.list(warehouseId), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("LOCATION_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody LocationCreateRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }
}
