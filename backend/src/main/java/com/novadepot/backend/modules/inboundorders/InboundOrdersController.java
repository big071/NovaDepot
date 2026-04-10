package com.novadepot.backend.modules.inboundorders;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inbound-orders")
public class InboundOrdersController {
    private final InboundOrdersService service;

    public InboundOrdersController(InboundOrdersService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("INBOUND_READ")
    public ApiResponse<List<InboundOrderEntity>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("INBOUND_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody InboundCreateRequest req) {
        return ApiResponse.success(service.create(req), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/approve")
    @RequirePermission("INBOUND_APPROVE")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        return ApiResponse.success(service.approve(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/post")
    @RequirePermission("INBOUND_POST")
    public ApiResponse<Map<String, Object>> post(@PathVariable Long id) {
        return ApiResponse.success(service.post(id), MDC.get("traceId"));
    }
}
