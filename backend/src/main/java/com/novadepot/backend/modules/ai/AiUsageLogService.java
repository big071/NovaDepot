package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AiUsageLogEntity;
import com.novadepot.backend.repository.AiUsageLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiUsageLogService {
    private static final Logger log = LoggerFactory.getLogger(AiUsageLogService.class);

    private final AiUsageLogMapper usageLogMapper;

    public AiUsageLogService(AiUsageLogMapper usageLogMapper) {
        this.usageLogMapper = usageLogMapper;
    }

    public void saveDegradationUsageLog(Map<String, Object> context,
                                        String scene,
                                        String providerName,
                                        long started,
                                        String reason) {
        try {
            AiUsageLogEntity usage = new AiUsageLogEntity();
            usage.setTenantId(RequestContext.tenantId());
            Object conversationId = context.get("conversationId");
            if (conversationId instanceof Long id) {
                usage.setConversationId(id);
            }
            usage.setProvider(providerName);
            usage.setModel(providerName);
            usage.setScene(scene);
            usage.setRole("user");
            usage.setPromptTokens(0);
            usage.setCompletionTokens(0);
            usage.setTotalTokens(0);
            usage.setLatencyMs((int) (System.currentTimeMillis() - started));
            usage.setSuccess(0);
            usage.setErrorCode("AI_DEGRADED");
            usage.setErrorMessage(truncate(reason, 512));
            usage.setCostEstimate(BigDecimal.ZERO);
            Object userId = context.get("userId");
            if (userId instanceof Long id) {
                usage.setCreatedBy(id);
            }
            usageLogMapper.insert(usage);
        } catch (Exception ex) {
            log.warn("Failed to save AI degradation usage log: {}", ex.getMessage());
        }
    }

    public void saveManualProviderUsageLog(Map<String, Object> context,
                                           String scene,
                                           String providerName,
                                           int latencyMs,
                                           boolean success,
                                           String errorCode,
                                           String errorMessage) {
        try {
            AiUsageLogEntity usage = new AiUsageLogEntity();
            usage.setTenantId(RequestContext.tenantId());
            Object conversationId = context.get("conversationId");
            if (conversationId instanceof Long id) {
                usage.setConversationId(id);
            }
            usage.setProvider(providerName);
            usage.setModel(providerName);
            usage.setScene(scene);
            usage.setRole("user");
            usage.setPromptTokens(0);
            usage.setCompletionTokens(0);
            usage.setTotalTokens(0);
            usage.setLatencyMs(latencyMs);
            usage.setSuccess(success ? 1 : 0);
            usage.setErrorCode(errorCode);
            usage.setErrorMessage(errorMessage);
            usage.setCostEstimate(BigDecimal.ZERO);
            Object userId = context.get("userId");
            if (userId instanceof Long id) {
                usage.setCreatedBy(id);
            }
            usageLogMapper.insert(usage);
        } catch (Exception ex) {
            log.warn("Failed to save manual AI provider usage log: {}", ex.getMessage());
        }
    }

    public List<Map<String, Object>> usageLogs(Long conversationId, int limit) {
        List<AiUsageLogEntity> list = usageLogMapper.selectRecentUsage(RequestContext.tenantId(), conversationId, Math.min(limit, 200));
        return list.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("conversationId", log.getConversationId());
            m.put("provider", log.getProvider());
            m.put("model", log.getModel());
            m.put("scene", log.getScene());
            m.put("promptTokens", log.getPromptTokens());
            m.put("completionTokens", log.getCompletionTokens());
            m.put("totalTokens", log.getTotalTokens());
            m.put("latencyMs", log.getLatencyMs());
            m.put("success", log.getSuccess());
            m.put("errorCode", log.getErrorCode());
            m.put("errorMessage", log.getErrorMessage());
            m.put("costEstimate", log.getCostEstimate());
            m.put("createdAt", log.getCreatedAt());
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
