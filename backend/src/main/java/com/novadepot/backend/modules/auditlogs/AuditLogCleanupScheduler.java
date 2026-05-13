package com.novadepot.backend.modules.auditlogs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuditLogCleanupScheduler {
    private final AuditLogCleanupService service;

    @Value("${app.audit.cleanup-enabled:true}")
    private boolean enabled;

    public AuditLogCleanupScheduler(AuditLogCleanupService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.audit.cleanup-cron:0 30 2 * * ?}")
    public void cleanup() {
        if (enabled) {
            service.cleanup();
        }
    }
}
