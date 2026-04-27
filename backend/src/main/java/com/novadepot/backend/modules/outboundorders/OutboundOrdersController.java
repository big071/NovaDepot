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

    @GetMapping("/{id}/items")
    @RequirePermission("OUTBOUND_READ")
    public ApiResponse<List<Map<String, Object>>> items(@PathVariable Long id) {
        return ApiResponse.success(service.items(id), MDC.get("traceId"));
    }

    @GetMapping("/{id}/detail")
    @RequirePermission("OUTBOUND_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("OUTBOUND_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody OutboundCreateRequest req) {
        return ApiResponse.success(service.create(req), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("OUTBOUND_UPDATE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody OutboundCreateRequest req) {
        return ApiResponse.success(service.update(id, req), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/submit")
    @RequirePermission("OUTBOUND_SUBMIT")
    public ApiResponse<Map<String, Object>> submit(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.submit(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/withdraw")
    @RequirePermission("OUTBOUND_WITHDRAW")
    public ApiResponse<Map<String, Object>> withdraw(@PathVariable Long id,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.withdraw(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/cancel")
    @RequirePermission("OUTBOUND_CANCEL")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.cancel(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/approve")
    @RequirePermission("OUTBOUND_APPROVE")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.approve(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/reject")
    @RequirePermission("OUTBOUND_APPROVE")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.reject(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/unapprove")
    @RequirePermission("OUTBOUND_UNAPPROVE")
    public ApiResponse<Map<String, Object>> unapprove(@PathVariable Long id,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.unapprove(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/ship")
    @RequirePermission("OUTBOUND_SHIP")
    public ApiResponse<Map<String, Object>> ship(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.ship(id, extractNote(body)), MDC.get("traceId"));
    }

    private String extractNote(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        if (body.get("note") != null) return String.valueOf(body.get("note"));
        if (body.get("remark") != null) return String.valueOf(body.get("remark"));
        if (body.get("reason") != null) return String.valueOf(body.get("reason"));
        if (body.get("comment") != null) return String.valueOf(body.get("comment"));
        if (body.get("opinion") != null) return String.valueOf(body.get("opinion"));
        return "";
    }
}
