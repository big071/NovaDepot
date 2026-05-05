package com.novadepot.backend.modules.sales;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales-orders")
public class SalesOrdersController {
    private final SalesOrdersService service;

    public SalesOrdersController(SalesOrdersService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("SALES_READ")
    public ApiResponse<List<SalesOrderEntity>> list(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Long partnerId) {
        return ApiResponse.success(service.list(status, partnerId), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("SALES_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("SALES_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody SalesOrderRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("SALES_UPDATE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody SalesOrderRequest request) {
        return ApiResponse.success(service.update(id, request), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/confirm")
    @RequirePermission("SALES_CONFIRM")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id) {
        return ApiResponse.success(service.confirm(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/cancel")
    @RequirePermission("SALES_CANCEL")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long id) {
        return ApiResponse.success(service.cancel(id), MDC.get("traceId"));
    }
}
