package com.novadepot.backend.modules.roles;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RolesService {
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of("id", 1, "name", "roles-sample-1", "tenantId", 1),
                Map.of("id", 2, "name", "roles-sample-2", "tenantId", 1)
        );
    }

    public Map<String, Object> detail(Long id) {
        return Map.of("id", id, "name", "roles-detail", "tenantId", 1);
    }
}
