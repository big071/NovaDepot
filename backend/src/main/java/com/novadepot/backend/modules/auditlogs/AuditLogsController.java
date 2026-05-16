package com.novadepot.backend.modules.auditlogs;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogsController {
    private final AuditLogsService service;
    private final AuditLogCleanupService cleanupService;

    public AuditLogsController(AuditLogsService service, AuditLogCleanupService cleanupService) {
        this.service = service;
        this.cleanupService = cleanupService;
    }

    @GetMapping
    @RequirePermission("AUDIT_READ")
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize,
                                                 @RequestParam(required = false) String module,
                                                 @RequestParam(required = false) String action,
                                                 @RequestParam(required = false) String resourceType,
                                                 @RequestParam(required = false) String resourceId,
                                                 @RequestParam(required = false) String bizNo,
                                                 @RequestParam(required = false) Long operatorId,
                                                 @RequestParam(required = false) String operatorKeyword,
                                                 @RequestParam(required = false) Boolean onlyFailed,
                                                 @RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo) {
        return ApiResponse.success(service.list(pageNo, pageSize, module, action, resourceType,
                resourceId, bizNo, operatorId, operatorKeyword, onlyFailed, dateFrom, dateTo), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("AUDIT_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping("/cleanup")
    @RequirePermission("AUDIT_CLEANUP_RUN")
    public ApiResponse<Map<String, Object>> cleanup() {
        return ApiResponse.success(cleanupService.cleanup(), MDC.get("traceId"));
    }
}
