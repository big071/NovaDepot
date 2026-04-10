package com.novadepot.backend.modules.notifications;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationsService {
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of("id", 1, "name", "notifications-sample-1", "tenantId", 1),
                Map.of("id", 2, "name", "notifications-sample-2", "tenantId", 1)
        );
    }

    public Map<String, Object> detail(Long id) {
        return Map.of("id", id, "name", "notifications-detail", "tenantId", 1);
    }
}
