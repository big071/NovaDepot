package com.novadepot.backend.modules.outboundorders;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/outbound-orders")
public class OutboundOrdersController {
    private final OutboundOrdersService service;

    public OutboundOrdersController(OutboundOrdersService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("OUTBOUND_READ")
    public ApiResponse<List<OutboundOrderEntity>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("OUTBOUND_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody OutboundCreateRequest req) {
        return ApiResponse.success(service.create(req), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/approve")
    @RequirePermission("OUTBOUND_APPROVE")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        return ApiResponse.success(service.approve(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/ship")
    @RequirePermission("OUTBOUND_SHIP")
    public ApiResponse<Map<String, Object>> ship(@PathVariable Long id) {
        return ApiResponse.success(service.ship(id), MDC.get("traceId"));
    }
}
