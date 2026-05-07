package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/usage-logs")
public class AiUsageLogController {
    private final AiService aiService;

    public AiUsageLogController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping
    @RequirePermission("AI_USAGE_LOG_VIEW")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long conversationId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(aiService.usageLogs(conversationId, limit), MDC.get("traceId"));
    }
}