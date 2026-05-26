package com.novadepot.backend.modules.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.config.AiProperties;
import com.novadepot.backend.model.entity.AiUsageLogEntity;
import com.novadepot.backend.repository.AiUsageLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekReasonerAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekReasonerAiProvider.class);

    private final AiProperties aiProperties;
    private final RestClient deepseekRestClient;
    private final AiUsageLogMapper usageLogMapper;
    private final ObjectMapper objectMapper;

    public DeepSeekReasonerAiProvider(AiProperties aiProperties,
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
        return "deepseek-reasoner";
    }

    @Override
    public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
        if (!aiProperties.isDeepseekEnabled()) {
            fail(context, scene, 0L, "DEEPSEEK_FAILED", null, null);
        }
        if (!StringUtils.hasText(aiProperties.getDeepseekApiKey())) {
            fail(context, scene, 0L, "DEEPSEEK_UNAUTHORIZED", null, null);
        }

        List<Map<String, String>> messages = new ArrayList<>();
        // system prompt
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", aiProperties.getSystemPrompt());
        messages.add(systemMsg);
        Object history = context.get("historyMessages");
        if (history instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> raw) {
                    Object role = raw.get("role");
                    Object content = raw.get("content");
                    if (role != null && content != null && StringUtils.hasText(String.valueOf(content))) {
                        Map<String, String> historyMsg = new HashMap<>();
                        historyMsg.put("role", String.valueOf(role));
                        historyMsg.put("content", String.valueOf(content));
                        messages.add(historyMsg);
                    }
                }
            }
        }

        // user message
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", message);
        messages.add(userMsg);

        Map<String, Object> body = Map.of(
                "model", aiProperties.getDeepseekReasonerModel(),
                "messages", messages,
                "stream", false,
                "max_tokens", 4096
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
            // reasoner may have reasoning_content in addition to content
            StringBuilder replyBuilder = new StringBuilder();
            JsonNode choice = root.path("choices").path(0);
            String reasoning = choice.path("message").path("reasoning_content").asText("");
            String content = choice.path("message").path("content").asText("");
            if (!reasoning.isEmpty()) {
                replyBuilder.append("【推理过程】\n").append(reasoning).append("\n\n【结论】\n");
            }
            replyBuilder.append(content);
            reply = replyBuilder.toString();

            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                promptTokens = usage.path("prompt_tokens").asInt(0);
                completionTokens = usage.path("completion_tokens").asInt(0);
                totalTokens = usage.path("total_tokens").asInt(0);
            }
        } catch (Exception e) {
            String failedCode = classifyErrorCode(e);
            Integer statusCode = httpStatus(e);
            log.warn("DeepSeek reasoner API call failed, errorCode={}, statusCode={}", failedCode, statusCode);
            fail(context, scene, started, failedCode, statusCode, e);
            return Map.of();
        }

        int latencyMs = (int) (System.currentTimeMillis() - started);
        BigDecimal costEstimate = estimateCost("deepseek-reasoner", promptTokens, completionTokens);

        // persist usage log
        saveUsageLog(context, scene, "user", promptTokens, completionTokens, totalTokens,
                latencyMs, success, errorCode, errorMessage, costEstimate);

        BigDecimal confidence = success ? BigDecimal.valueOf(0.90) : BigDecimal.valueOf(0.1);

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", confidence,
                "model", aiProperties.getDeepseekReasonerModel(),
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
            log.setModel(aiProperties.getDeepseekReasonerModel());
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

    private void fail(Map<String, Object> context,
                      String scene,
                      long started,
                      String errorCode,
                      Integer statusCode,
                      Throwable cause) {
        int latencyMs = started == 0L ? 0 : (int) (System.currentTimeMillis() - started);
        String safeMessage = deepSeekFailureMessage();
        saveUsageLog(context, scene, "user", 0, 0, 0, latencyMs,
                false, errorCode, safeMessage, BigDecimal.ZERO);
        throw new AiProviderCallException(providerName(), aiProperties.getDeepseekReasonerModel(),
                errorCode, statusCode, safeMessage, cause);
    }

    private String classifyErrorCode(Throwable e) {
        Integer status = httpStatus(e);
        if (status != null) {
            if (status == 401 || status == 403) {
                return "DEEPSEEK_UNAUTHORIZED";
            }
            if (status == 408 || status == 504) {
                return "DEEPSEEK_TIMEOUT";
            }
            if (status == 429) {
                return "DEEPSEEK_QUOTA_EXCEEDED";
            }
        }
        if (e instanceof ResourceAccessException) {
            return "DEEPSEEK_TIMEOUT";
        }
        String text = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (text.contains("quota") || text.contains("balance") || text.contains("insufficient")) {
            return "DEEPSEEK_QUOTA_EXCEEDED";
        }
        return "DEEPSEEK_FAILED";
    }

    private Integer httpStatus(Throwable e) {
        if (e instanceof HttpStatusCodeException http) {
            return http.getStatusCode().value();
        }
        Throwable cause = e.getCause();
        if (cause instanceof HttpStatusCodeException http) {
            return http.getStatusCode().value();
        }
        return null;
    }

    private String deepSeekFailureMessage() {
        return """
                DeepSeek 调用失败，请检查：
                1. API Key 是否正确
                2. AI_DEEPSEEK_ENABLED 是否为 true
                3. AI_PROVIDER 是否为 deepseek-chat 或 deepseek-reasoner
                4. 网络是否能访问 DeepSeek
                5. 账户额度是否充足
                6. 模型名称是否正确
                """;
    }
}
