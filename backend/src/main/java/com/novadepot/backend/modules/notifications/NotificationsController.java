package com.novadepot.backend.modules.notifications;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationsController {
    private final NotificationsService service;

    public NotificationsController(NotificationsService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission("NOTIFY_READ")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) Boolean unreadOnly,
                                                 @RequestParam(required = false) Integer pageNo,
                                                 @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(service.list(unreadOnly, pageNo, pageSize), MDC.get("traceId"));
    }

    @GetMapping("/unread-count")
    @RequirePermission("NOTIFY_READ")
    public ApiResponse<Map<String, Object>> unreadCount() {
        return ApiResponse.success(service.unreadCount(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    @RequirePermission("NOTIFY_READ")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id), MDC.get("traceId"));
    }

    @PostMapping("/{id}/read")
    @RequirePermission("NOTIFY_UPDATE")
    public ApiResponse<Map<String, Object>> markRead(@PathVariable Long id) {
        return ApiResponse.success(service.markRead(id), MDC.get("traceId"));
    }

    @PostMapping("/read-all")
    @RequirePermission("NOTIFY_UPDATE")
    public ApiResponse<Map<String, Object>> markAllRead() {
        return ApiResponse.success(service.markAllRead(), MDC.get("traceId"));
    }
}
