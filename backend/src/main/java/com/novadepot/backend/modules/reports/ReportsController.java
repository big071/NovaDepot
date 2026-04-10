package com.novadepot.backend.modules.reports;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {
    private final ReportsService service;

    public ReportsController(ReportsService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @RequirePermission("REPORT_DASHBOARD_READ")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(service.dashboard(), MDC.get("traceId"));
    }
}
