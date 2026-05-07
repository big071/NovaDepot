package com.novadepot.backend.modules.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.AIConversationEntity;
import com.novadepot.backend.model.entity.AIMessageEntity;
import com.novadepot.backend.model.entity.AIPromptTemplateEntity;
import com.novadepot.backend.model.entity.AiUsageLogEntity;
import com.novadepot.backend.modules.ai.provider.AiProvider;
import com.novadepot.backend.modules.knowledge.KnowledgeService;
import com.novadepot.backend.repository.AIConversationMapper;
import com.novadepot.backend.repository.AIMessageMapper;
import com.novadepot.backend.repository.AIPromptTemplateMapper;
import com.novadepot.backend.repository.AiUsageLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final List<AiProvider> providers;
    private final AIConversationMapper conversationMapper;
    private final AIMessageMapper messageMapper;
    private final AIPromptTemplateMapper promptTemplateMapper;
    private final KnowledgeService knowledgeService;
    private final AiUsageLogMapper usageLogMapper;
    private final String defaultProvider;
    private final boolean paidEnabled;

    public AiService(List<AiProvider> providers,
                     AIConversationMapper conversationMapper,
                     AIMessageMapper messageMapper,
                     AIPromptTemplateMapper promptTemplateMapper,
                     KnowledgeService knowledgeService,
                     AiUsageLogMapper usageLogMapper,
                     @Value("${app.ai.provider:rule}") String defaultProvider,
                     @Value("${app.ai.paid-enabled:false}") boolean paidEnabled) {
        this.providers = providers;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.knowledgeService = knowledgeService;
        this.usageLogMapper = usageLogMapper;
        this.defaultProvider = defaultProvider;
        this.paidEnabled = paidEnabled;
    }

    public Map<String, Object> chat(AiChatRequest request) {
        String scene = normalizeScene(request.getScene());
        String userMessage = request.getMessage().trim();

        String resolvedProviderName = resolveProviderName(request.getProviderHint());
        AIConversationEntity conversation = resolveConversation(request.getConversationId(), scene, resolvedProviderName);

        saveMessage(conversation.getId(), "USER", userMessage, null, null, null, null);

        AiProvider provider = resolveProvider(resolvedProviderName, scene);
        String providerInput = "rule".equalsIgnoreCase(provider.providerName())
                ? userMessage
                : renderPrompt(scene, userMessage);

        Map<String, Object> context = new HashMap<>();
        context.put("conversationId", conversation.getId());
        context.put("tenantId", RequestContext.tenantId());
        context.put("userId", RequestContext.userId());
        context.put("scene", scene);

        long started = System.currentTimeMillis();
        Map<String, Object> providerResp;
        List<Map<String, Object>> knowledgeRefs = knowledgeService.matchKnowledge(userMessage, knowledgeScene(scene));
        String fallbackFrom = null;
        try {
            providerResp = provider.chat(scene, providerInput, context);
        } catch (Exception ex) {
            log.warn("AI provider failed, provider={}, scene={}, reason={}", provider.providerName(), scene, ex.getMessage());
            fallbackFrom = provider.providerName();
            saveDegradationUsageLog(context, scene, fallbackFrom, started, ex.getMessage());
            try {
                provider = resolveProvider("rule", scene);
                providerResp = provider.chat(scene, userMessage, context);
            } catch (Exception ruleEx) {
                log.warn("AI rule fallback failed, scene={}, reason={}", scene, ruleEx.getMessage());
                providerResp = fallbackToMock(scene, userMessage, context, ruleEx.getMessage());
                provider = resolveProvider("mock", scene);
            }
        }
        int latencyMs = (int) (System.currentTimeMillis() - started);

        String reply = String.valueOf(providerResp.getOrDefault("reply", "系统繁忙，请稍后重试。"));
        BigDecimal confidence = toBigDecimal(providerResp.get("confidence"));
        Integer tokens = toInteger(providerResp.get("tokens"));
        String errorCode = fallbackFrom == null ? null : "AI_PROVIDER_FALLBACK";

        saveMessage(conversation.getId(), "ASSISTANT", reply, confidence, tokens, latencyMs, errorCode);
        conversation.setProviderType(provider.providerName());
        Object model = providerResp.get("model");
        if (model != null) {
            conversation.setModelName(String.valueOf(model));
        }
        conversationMapper.updateById(conversation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("conversationNo", conversation.getConversationNo());
        result.put("scene", scene);
        result.put("provider", provider.providerName());
        result.put("reply", reply);
        result.put("confidence", confidence);
        result.put("tokens", tokens);
        result.put("latencyMs", latencyMs);
        if (fallbackFrom != null) {
            result.put("fallbackFrom", fallbackFrom);
        }
        if (providerResp.containsKey("metrics")) {
            result.put("metrics", providerResp.get("metrics"));
        }
        if (providerResp.containsKey("suggestions")) {
            result.put("suggestions", providerResp.get("suggestions"));
        }
        if (providerResp.containsKey("usage")) {
            result.put("usage", providerResp.get("usage"));
        }
        result.put("knowledgeRefs", knowledgeRefs);
        result.put("knowledgeHit", !knowledgeRefs.isEmpty());
        result.put("knowledgeFallbackNotice", knowledgeRefs.isEmpty() ? "未命中知识库，当前回答来自规则或模拟提供者。" : "");
        return result;
    }

    public List<Map<String, Object>> conversations() {
        List<AIConversationEntity> list = conversationMapper.selectList(new LambdaQueryWrapper<AIConversationEntity>()
                .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(AIConversationEntity::getStartedAt)
                .last("limit 50"));

        return list.stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "conversationNo", c.getConversationNo(),
                "scene", c.getScene(),
                "provider", c.getProviderType(),
                "status", c.getStatus(),
                "startedAt", c.getStartedAt()
        )).toList();
    }

    public List<Map<String, Object>> conversationMessages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<AIMessageEntity>()
                        .eq(AIMessageEntity::getTenantId, RequestContext.tenantId())
                        .eq(AIMessageEntity::getConversationId, conversationId)
                        .orderByAsc(AIMessageEntity::getId))
                .stream()
                .map(message -> Map.<String, Object>of(
                        "id", message.getId(),
                        "conversationId", message.getConversationId(),
                        "role", message.getRole(),
                        "content", message.getContent(),
                        "tokens", message.getTokens() == null ? 0 : message.getTokens(),
                        "latencyMs", message.getLatencyMs() == null ? 0 : message.getLatencyMs(),
                        "confidence", message.getConfidence() == null ? BigDecimal.ZERO : message.getConfidence(),
                        "errorCode", message.getErrorCode() == null ? "" : message.getErrorCode(),
                        "createdAt", message.getCreatedAt()
                )).toList();
    }

    public List<Map<String, Object>> conversationMessagesByNo(String conversationNo) {
        AIConversationEntity conversation = conversationMapper.selectOne(new LambdaQueryWrapper<AIConversationEntity>()
                .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                .eq(AIConversationEntity::getConversationNo, conversationNo)
                .last("limit 1"));
        if (conversation == null || conversation.getId() == null) {
            return List.of();
        }
        return conversationMessages(conversation.getId());
    }

    private AIConversationEntity resolveConversation(Long conversationId, String scene, String providerName) {
        if (conversationId != null) {
            AIConversationEntity existed = conversationMapper.selectOne(new LambdaQueryWrapper<AIConversationEntity>()
                    .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                    .eq(AIConversationEntity::getId, conversationId));
            if (existed != null) {
                return existed;
            }
        }

        AIConversationEntity created = new AIConversationEntity();
        created.setTenantId(RequestContext.tenantId());
        created.setConversationNo(NoGenerator.next("AI"));
        created.setScene(scene);
        created.setProviderType(providerName);
        created.setStatus("ACTIVE");
        created.setStartedAt(LocalDateTime.now());
        created.setCreatedBy(RequestContext.userId());
        created.setUpdatedBy(RequestContext.userId());
        conversationMapper.insert(created);
        return created;
    }

    private String renderPrompt(String scene, String userMessage) {
        AIPromptTemplateEntity template = promptTemplateMapper.selectOne(new LambdaQueryWrapper<AIPromptTemplateEntity>()
                .eq(AIPromptTemplateEntity::getTenantId, RequestContext.tenantId())
                .eq(AIPromptTemplateEntity::getScene, scene)
                .eq(AIPromptTemplateEntity::getEnabled, 1)
                .orderByDesc(AIPromptTemplateEntity::getVersionNo)
                .last("limit 1"));

        String content;
        if (template != null && StringUtils.hasText(template.getTemplateContent())) {
            content = template.getTemplateContent();
        } else {
            content = defaultPromptByScene(scene);
        }

        if (content.contains("{{question}}")) {
            return content.replace("{{question}}", userMessage);
        }
        return content + "\n用户问题：" + userMessage;
    }

    private String defaultPromptByScene(String scene) {
        return switch (scene) {
            case "warehouse" -> "你是NovaDepot仓库助手，请基于库存与单据事实给出可执行建议。\n问题：{{question}}";
            case "sop" -> "你是NovaDepot SOP助手，请给出标准作业步骤、风险点与复核动作。\n问题：{{question}}";
            case "enterprise" -> "你是NovaDepot企业助手，请先给结论，再给执行建议。\n问题：{{question}}";
            default -> "你是NovaDepot AI助手，请输出结构化、可执行建议。\n问题：{{question}}";
        };
    }

    private String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "enterprise";
        }
        return scene.trim().toLowerCase();
    }

    private String knowledgeScene(String scene) {
        return "sop".equalsIgnoreCase(scene) ? "customer-service" : scene;
    }

    private String resolveProviderName(String providerHint) {
        String target = StringUtils.hasText(providerHint) ? providerHint.trim().toLowerCase() : defaultProvider;
        if ("paid".equals(target) && !paidEnabled) {
            return "rule";
        }
        return target;
    }

    private AiProvider resolveProvider(String providerName, String scene) {
        return providers.stream()
                .filter(p -> p.providerName().equalsIgnoreCase(providerName))
                .filter(p -> p.supportsScene(scene))
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(p -> p.providerName().equalsIgnoreCase(defaultProvider))
                        .findFirst()
                        .orElseGet(() -> providers.get(0)));
    }

    private Map<String, Object> fallbackToMock(String scene,
                                               String userMessage,
                                               Map<String, Object> context,
                                               String reason) {
        AiProvider mock = resolveProvider("mock", scene);
        try {
            Map<String, Object> fallback = new HashMap<>(mock.chat(scene, userMessage, context));
            fallback.put("fallbackReason", reason);
            return fallback;
        } catch (Exception ignored) {
            return Map.of(
                    "reply", "系统繁忙，请稍后重试。",
                    "scene", scene,
                    "provider", "system",
                    "confidence", 0.1
            );
        }
    }

    private void saveMessage(Long conversationId,
                             String role,
                             String content,
                             BigDecimal confidence,
                             Integer tokens,
                             Integer latencyMs,
                             String errorCode) {
        AIMessageEntity message = new AIMessageEntity();
        message.setTenantId(RequestContext.tenantId());
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setConfidence(confidence);
        message.setTokens(tokens);
        message.setLatencyMs(latencyMs);
        message.setErrorCode(errorCode);
        message.setCreatedBy(RequestContext.userId());
        message.setUpdatedBy(RequestContext.userId());
        messageMapper.insert(message);
    }

    private void saveDegradationUsageLog(Map<String, Object> context,
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

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    public List<Map<String, Object>> usageLogs(Long conversationId, int limit) {
        List<AiUsageLogEntity> list = usageLogMapper.selectList(new LambdaQueryWrapper<AiUsageLogEntity>()
                .eq(conversationId != null, AiUsageLogEntity::getConversationId, conversationId)
                .eq(AiUsageLogEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(AiUsageLogEntity::getId)
                .last("limit " + Math.min(limit, 200)));
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

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
