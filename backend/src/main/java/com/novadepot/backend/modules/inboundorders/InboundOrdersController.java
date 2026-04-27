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

    @GetMapping("/{id}/items")
    @RequirePermission("INBOUND_READ")
    public ApiResponse<List<Map<String, Object>>> items(@PathVariable Long id) {
        return ApiResponse.success(service.items(id), MDC.get("traceId"));
    }

    @GetMapping("/{id}/detail")
    @RequirePermission("INBOUND_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping
    @RequirePermission("INBOUND_CREATE")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody InboundCreateRequest req) {
        return ApiResponse.success(service.create(req), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @RequirePermission("INBOUND_UPDATE")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
                                                   @Valid @RequestBody InboundCreateRequest req) {
        return ApiResponse.success(service.update(id, req), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/submit")
    @RequirePermission("INBOUND_SUBMIT")
    public ApiResponse<Map<String, Object>> submit(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.submit(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/withdraw")
    @RequirePermission("INBOUND_WITHDRAW")
    public ApiResponse<Map<String, Object>> withdraw(@PathVariable Long id,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.withdraw(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/cancel")
    @RequirePermission("INBOUND_CANCEL")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.cancel(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/approve")
    @RequirePermission("INBOUND_APPROVE")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.approve(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/reject")
    @RequirePermission("INBOUND_APPROVE")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.reject(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/unapprove")
    @RequirePermission("INBOUND_UNAPPROVE")
    public ApiResponse<Map<String, Object>> unapprove(@PathVariable Long id,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.unapprove(id, extractNote(body)), MDC.get("traceId"));
    }

    @PostMapping("/{id}/actions/post")
    @RequirePermission("INBOUND_POST")
    public ApiResponse<Map<String, Object>> post(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(service.post(id, extractNote(body)), MDC.get("traceId"));
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
