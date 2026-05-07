package com.novadepot.backend.modules.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.config.AiProperties;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AiUsageLogEntity;
import com.novadepot.backend.repository.AiUsageLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekChatAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatAiProvider.class);

    private final AiProperties aiProperties;
    private final RestClient deepseekRestClient;
    private final AiUsageLogMapper usageLogMapper;
    private final ObjectMapper objectMapper;

    public DeepSeekChatAiProvider(AiProperties aiProperties,
                                  RestClient deepseekRestClient,
                                  AiUsageLogMapper usageLogMapper,
                                  ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.deepseekRestClient = deepseekRestClient;
        this.usageLogMapper = usageLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "deepseek-chat";
    }

    @Override
    public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
        if (!aiProperties.isDeepseekEnabled()) {
            throw new IllegalStateException("DeepSeek chat provider is disabled");
        }
        if (!StringUtils.hasText(aiProperties.getDeepseekApiKey())) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        // system prompt
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", aiProperties.getSystemPrompt());
        messages.add(systemMsg);

        // user message
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", message);
        messages.add(userMsg);

        Map<String, Object> body = Map.of(
                "model", aiProperties.getDeepseekChatModel(),
                "messages", messages,
                "stream", false,
                "max_tokens", 2048,
                "temperature", 0.7
        );

        long started = System.currentTimeMillis();
        boolean success = true;
        String errorCode = null;
        String errorMessage = null;
        String reply = "";
        Integer promptTokens = 0;
        Integer completionTokens = 0;
        Integer totalTokens = 0;

        try {
            String responseJson = deepseekRestClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            reply = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                promptTokens = usage.path("prompt_tokens").asInt(0);
                completionTokens = usage.path("completion_tokens").asInt(0);
                totalTokens = usage.path("total_tokens").asInt(0);
            }
        } catch (Exception e) {
            log.warn("DeepSeek chat API call failed: {}", e.getMessage());
            throw new IllegalStateException("DeepSeek chat API call failed: " + truncate(e.getMessage(), 256), e);
        }

        int latencyMs = (int) (System.currentTimeMillis() - started);
        BigDecimal costEstimate = estimateCost("deepseek-chat", promptTokens, completionTokens);

        // persist usage log
        saveUsageLog(context, scene, "user", promptTokens, completionTokens, totalTokens,
                latencyMs, success, errorCode, errorMessage, costEstimate);

        BigDecimal confidence = success ? BigDecimal.valueOf(0.88) : BigDecimal.valueOf(0.1);

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", confidence,
                "model", aiProperties.getDeepseekChatModel(),
                "tokens", totalTokens,
                "usage", Map.of(
                        "promptTokens", promptTokens,
                        "completionTokens", completionTokens,
                        "totalTokens", totalTokens,
                        "latencyMs", latencyMs,
                        "costEstimate", costEstimate
                )
        );
    }

    @SuppressWarnings("unchecked")
    private void saveUsageLog(Map<String, Object> context, String scene, String role,
                              int promptTokens, int completionTokens, int totalTokens,
                              int latencyMs, boolean success, String errorCode,
                              String errorMessage, BigDecimal costEstimate) {
        try {
            AiUsageLogEntity log = new AiUsageLogEntity();
            log.setTenantId(context.containsKey("tenantId") ? (Long) context.get("tenantId") : 0L);
            Object convId = context.get("conversationId");
            if (convId instanceof Long lid) {
                log.setConversationId(lid);
            }
            log.setProvider(providerName());
            log.setModel(aiProperties.getDeepseekChatModel());
            log.setScene(scene);
            log.setRole(role);
            log.setPromptTokens(promptTokens);
            log.setCompletionTokens(completionTokens);
            log.setTotalTokens(totalTokens);
            log.setLatencyMs(latencyMs);
            log.setSuccess(success ? 1 : 0);
            log.setErrorCode(errorCode);
            log.setErrorMessage(errorMessage);
            log.setCostEstimate(costEstimate);
            Object userId = context.get("userId");
            if (userId instanceof Long uid) {
                log.setCreatedBy(uid);
            }
            usageLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("Failed to save AI usage log", e);
        }
    }

    private BigDecimal estimateCost(String model, int promptTokens, int completionTokens) {
        // DeepSeek pricing (CNY per 1M tokens): chat input 1, output 2; reasoner input 4, output 16
        double inputPrice = model.contains("reasoner") ? 4.0 : 1.0;
        double outputPrice = model.contains("reasoner") ? 16.0 : 2.0;
        double cost = (promptTokens / 1_000_000.0) * inputPrice
                + (completionTokens / 1_000_000.0) * outputPrice;
        return BigDecimal.valueOf(cost).setScale(6, RoundingMode.HALF_UP);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
