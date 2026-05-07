package com.novadepot.backend.modules.purchase;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.PurchaseOrderEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrdersController {
    private final PurchaseOrdersService service;

    public PurchaseOrdersController(PurchaseOrdersService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("PURCHASE_READ")
    public ApiResponse<List<PurchaseOrderEntity>> list(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Long partnerId) {
        return ApiResponse.success(service.list(status, partnerId), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("PURCHASE_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("PURCHASE_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.success(service.create(request), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("PURCHASE_UPDATE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.success(service.update(id, request), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/confirm")
    @RequirePermission("PURCHASE_CONFIRM")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id) {
        return ApiResponse.success(service.confirm(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/cancel")
    @RequirePermission("PURCHASE_CANCEL")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long id) {
        return ApiResponse.success(service.cancel(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/create-inbound-draft")
    @RequirePermission("PURCHASE_TO_INBOUND")
    public ApiResponse<Map<String, Object>> createInboundDraft(@PathVariable Long id,
                                                               @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.createInboundDraft(id, extractLong(body, "locationId")), MDC.get("traceId"));
    }

    private Long extractLong(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object value = body.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
