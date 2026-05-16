package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.common.config.AiProperties;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/config")
public class AiConfigController {
    private final AiProperties aiProperties;

    public AiConfigController(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @GetMapping
    @RequirePermission("AI_CONFIG_VIEW")
    public ApiResponse<Map<String, Object>> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("defaultProvider", aiProperties.getProvider());
        result.put("deepseekEnabled", aiProperties.isDeepseekEnabled());
        result.put("deepseekBaseUrl", aiProperties.getDeepseekBaseUrl());
        result.put("deepseekChatModel", aiProperties.getDeepseekChatModel());
        result.put("deepseekReasonerModel", aiProperties.getDeepseekReasonerModel());
        result.put("deepseekApiKeyMasked", maskApiKey(aiProperties.getDeepseekApiKey()));
        result.put("paidEnabled", aiProperties.isPaidEnabled());
        return ApiResponse.success(result, MDC.get("traceId"));
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
