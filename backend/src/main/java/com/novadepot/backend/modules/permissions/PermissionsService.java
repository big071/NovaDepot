package com.novadepot.backend.modules.permissions;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PermissionsService {
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of("id", 1, "name", "permissions-sample-1", "tenantId", 1),
                Map.of("id", 2, "name", "permissions-sample-2", "tenantId", 1)
        );
    }

    public Map<String, Object> detail(Long id) {
        return Map.of("id", id, "name", "permissions-detail", "tenantId", 1);
    }
}
