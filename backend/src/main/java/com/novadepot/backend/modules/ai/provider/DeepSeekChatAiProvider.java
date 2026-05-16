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

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getDeepseekChatModel());
        body.put("messages", messages);
        body.put("stream", false);
        body.put("max_tokens", 2048);
        body.put("temperature", 0.7);
        Object tools = context.get("aiTools");
        if (tools instanceof List<?> list && !list.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", context.getOrDefault("toolChoice", "auto"));
        }

        long started = System.currentTimeMillis();
        String reply = "";
        Integer promptTokens = 0;
        Integer completionTokens = 0;
        Integer totalTokens = 0;
        List<Map<String, Object>> toolCalls = new ArrayList<>();

        try {
            String responseJson = deepseekRestClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode messageNode = root.path("choices").path(0).path("message");
            reply = messageNode.path("content").asText("");
            JsonNode toolCallNodes = messageNode.path("tool_calls");
            if (toolCallNodes.isArray()) {
                for (JsonNode node : toolCallNodes) {
                    JsonNode fn = node.path("function");
                    if (StringUtils.hasText(fn.path("name").asText())) {
                        toolCalls.add(Map.of(
                                "name", fn.path("name").asText(),
                                "arguments", fn.path("arguments").asText("{}")
                        ));
                    }
                }
            }
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                promptTokens = usage.path("prompt_tokens").asInt(0);
                completionTokens = usage.path("completion_tokens").asInt(0);
                totalTokens = usage.path("total_tokens").asInt(0);
            }
        } catch (Exception e) {
            String failedCode = classifyErrorCode(e);
            Integer statusCode = httpStatus(e);
            log.warn("DeepSeek chat API call failed, errorCode={}, statusCode={}", failedCode, statusCode);
            fail(context, scene, started, failedCode, statusCode, e);
            return Map.of();
        }

        int latencyMs = (int) (System.currentTimeMillis() - started);
        BigDecimal costEstimate = estimateCost("deepseek-chat", promptTokens, completionTokens);

        // persist usage log
        saveUsageLog(context, scene, "user", promptTokens, completionTokens, totalTokens,
                latencyMs, true, null, null, costEstimate);

        BigDecimal confidence = BigDecimal.valueOf(0.88);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("scene", scene);
        result.put("provider", providerName());
        result.put("confidence", confidence);
        result.put("model", aiProperties.getDeepseekChatModel());
        result.put("tokens", totalTokens);
        result.put("toolCalls", toolCalls);
        result.put("usage", Map.of(
                "promptTokens", promptTokens,
                "completionTokens", completionTokens,
                "totalTokens", totalTokens,
                "latencyMs", latencyMs,
                "costEstimate", costEstimate
        ));
        return result;
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
        throw new AiProviderCallException(providerName(), aiProperties.getDeepseekChatModel(),
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
                3. AI_PROVIDER 是否为 deepseek-chat
                4. 网络是否能访问 DeepSeek
                5. 账户额度是否充足
                6. 模型名称是否正确
                """;
    }
}
