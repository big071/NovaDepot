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
        result.put("fallbackEnabled", aiProperties.isFallbackEnabled());
        result.put("toolsEnabled", aiProperties.isToolsEnabled());
        result.put("activeModel", activeModel());
        result.put("providerStatus", providerStatus());
        result.put("fallbackStatus", aiProperties.isFallbackEnabled() ? "ENABLED" : "DISABLED");
        result.put("systemPromptPreview", preview(aiProperties.getSystemPrompt(), 600));
        return ApiResponse.success(result, MDC.get("traceId"));
    }

    private String activeModel() {
        String provider = aiProperties.getProvider();
        if ("deepseek-reasoner".equalsIgnoreCase(provider)) {
            return aiProperties.getDeepseekReasonerModel();
        }
        if ("deepseek-chat".equalsIgnoreCase(provider)) {
            return aiProperties.getDeepseekChatModel();
        }
        if ("paid".equalsIgnoreCase(provider)) {
            return aiProperties.getModel();
        }
        return provider;
    }

    private String providerStatus() {
        String provider = aiProperties.getProvider();
        if (provider != null && provider.toLowerCase().startsWith("deepseek")) {
            return aiProperties.isDeepseekEnabled() ? "READY" : "DISABLED";
        }
        if ("rule".equalsIgnoreCase(provider)) {
            return "RULE_ONLY";
        }
        return "CONFIGURED";
    }

    private String preview(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
