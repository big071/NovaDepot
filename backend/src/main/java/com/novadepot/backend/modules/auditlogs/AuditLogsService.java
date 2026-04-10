package com.novadepot.backend.modules.auditlogs;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuditLogsService {
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of("id", 1, "name", "auditLogs-sample-1", "tenantId", 1),
                Map.of("id", 2, "name", "auditLogs-sample-2", "tenantId", 1)
        );
    }

    public Map<String, Object> detail(Long id) {
        return Map.of("id", id, "name", "auditLogs-detail", "tenantId", 1);
    }
}
