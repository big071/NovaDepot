package com.novadepot.backend.modules.auditlogs;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogsController {
    private final AuditLogsService service;

    public AuditLogsController(AuditLogsService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("AUDIT_READ")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("AUDIT_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }
}
