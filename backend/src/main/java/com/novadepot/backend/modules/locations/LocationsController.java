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

    @GetMapping("/{id}")
    @RequirePermission("LOCATION_READ")
    public ApiResponse<WarehouseLocationEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @GetMapping("/code/{locationCode}")
    @RequirePermission("LOCATION_READ")
    public ApiResponse<WarehouseLocationEntity> detailByCode(@PathVariable String locationCode) {
        return ApiResponse.success(service.detailByCode(locationCode), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("LOCATION_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody LocationCreateRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("LOCATION_UPDATE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody LocationCreateRequest request) {
        return ApiResponse.success(service.update(id, request), MDC.get("traceId"));
    }
}
