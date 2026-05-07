package com.novadepot.backend.modules.backup;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.model.entity.BackupRecordEntity;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/backups")
public class BackupController {
    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("BACKUP_READ")
    public ApiResponse<List<BackupRecordEntity>> list() {
        return ApiResponse.success(service.list(), MDC.get("traceId"));
    }

    @PostMapping("/actions/run")
    @RequirePermission("BACKUP_RUN")
    public ApiResponse<Map<String, Object>> run() {
        return ApiResponse.success(service.runManualBackup(), MDC.get("traceId"));
    }
}
