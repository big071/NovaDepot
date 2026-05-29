package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AIConversationEntity;
import com.novadepot.backend.modules.ai.provider.AiProvider;
import com.novadepot.backend.modules.ai.provider.AiProviderCallException;
import com.novadepot.backend.modules.ai.provider.AiProviderResponse;
import com.novadepot.backend.modules.ai.provider.AiProviderResponseMapper;
import com.novadepot.backend.modules.ai.tools.AiFunctionCallingOrchestrator;
import com.novadepot.backend.modules.ai.tools.AiFunctionCallingResult;
import com.novadepot.backend.modules.knowledge.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final AiProviderResolver providerResolver;
    private final AiPromptService promptService;
    private final AiUsageLogService usageLogService;
    private final AiConversationService conversationService;
    private final AiMessageService messageService;
    private final KnowledgeService knowledgeService;
    private final AiFunctionCallingOrchestrator functionCallingOrchestrator;
    private final AiProviderResponseMapper providerResponseMapper;
    private final boolean fallbackEnabled;

    public AiChatService(AiProviderResolver providerResolver,
                         AiPromptService promptService,
                         AiUsageLogService usageLogService,
                         AiConversationService conversationService,
                         AiMessageService messageService,
                         KnowledgeService knowledgeService,
                         AiFunctionCallingOrchestrator functionCallingOrchestrator,
                         AiProviderResponseMapper providerResponseMapper,
                         @Value("${app.ai.fallback-enabled:false}") boolean fallbackEnabled) {
        this.providerResolver = providerResolver;
        this.promptService = promptService;
        this.usageLogService = usageLogService;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.knowledgeService = knowledgeService;
        this.functionCallingOrchestrator = functionCallingOrchestrator;
        this.providerResponseMapper = providerResponseMapper;
        this.fallbackEnabled = fallbackEnabled;
    }

    public Map<String, Object> chat(AiChatRequest request) {
        String scene = promptService.normalizeScene(request.getScene());
        String userMessage = request.getMessage().trim();

        String resolvedProviderName = providerResolver.resolveProviderName(request.getProviderHint());
        AIConversationEntity conversation = conversationService.resolveConversation(request.getConversationId(), scene, resolvedProviderName);
        conversationService.ensureWritable(conversation);

        List<Map<String, String>> historyMessages = messageService.recentContextMessages(conversation.getId());
        messageService.saveMessage(conversation.getId(), "USER", userMessage, null, null, null, null, "COMPLETED");

        AiProvider provider = providerResolver.resolveProvider(resolvedProviderName, scene);
        String providerInput = "rule".equalsIgnoreCase(provider.providerName())
                ? userMessage
                : promptService.renderPrompt(scene, userMessage);

        Map<String, Object> context = new HashMap<>();
        context.put("conversationId", conversation.getId());
        context.put("tenantId", RequestContext.tenantId());
        context.put("userId", RequestContext.userId());
        context.put("scene", scene);
        context.put("historyMessages", historyMessages);
        functionCallingOrchestrator.prepareContext(context);

        long started = System.currentTimeMillis();
        AiProviderResponse providerResp;
        List<Map<String, Object>> knowledgeRefs = knowledgeService.matchKnowledge(userMessage, promptService.knowledgeScene(scene));
        String fallbackFrom = null;
        try {
            providerResp = provider.chat(scene, providerInput, context);
        } catch (Exception ex) {
            log.warn("AI provider failed, provider={}, scene={}, reason={}", provider.providerName(), scene, ex.getMessage());
            if (!fallbackEnabled) {
                return providerFailureResult(conversation, provider, scene, started, ex, knowledgeRefs);
            }
            fallbackFrom = provider.providerName();
            usageLogService.saveDegradationUsageLog(context, scene, fallbackFrom, started, ex.getMessage());
            try {
                provider = providerResolver.resolveProvider("rule", scene);
                providerResp = provider.chat(scene, userMessage, context);
            } catch (Exception ruleEx) {
                log.warn("AI rule fallback failed, scene={}, reason={}", scene, ruleEx.getMessage());
                providerResp = fallbackToMock(scene, userMessage, context, ruleEx.getMessage());
                provider = providerResolver.resolveProvider("mock", scene);
            }
        }
        int latencyMs = (int) (System.currentTimeMillis() - started);
        if (!providerResolver.isDeepSeekProvider(provider.providerName())) {
            usageLogService.saveManualProviderUsageLog(context, scene, provider.providerName(), latencyMs, true, null, null);
        }

        String reply = providerResp.reply() == null || providerResp.reply().isBlank()
                ? "系统繁忙，请稍后重试。"
                : providerResp.reply();
        AiFunctionCallingResult toolResult = functionCallingOrchestrator.run(
                userMessage, context, providerResp, conversation.getId(), null, "sync-" + System.currentTimeMillis());
        if (toolResult.usedTools()) {
            reply = toolResult.reply();
        }
        BigDecimal confidence = providerResp.confidence();
        Integer tokens = providerResp.tokens();
        String errorCode = fallbackFrom == null ? null : "AI_PROVIDER_FALLBACK";

        messageService.saveMessage(conversation.getId(), "ASSISTANT", reply, confidence, tokens, latencyMs, errorCode, "COMPLETED");
        conversationService.touchConversation(conversation, provider.providerName(), providerResp.model());

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
        if (!providerResp.metrics().isEmpty()) {
            result.put("metrics", providerResp.metrics());
        }
        if (!providerResp.suggestions().isEmpty()) {
            result.put("suggestions", providerResp.suggestions());
        }
        if (providerResp.usage() != null) {
            result.put("usage", providerResponseMapper.usageToMap(providerResp.usage()));
        }
        result.put("toolCalls", toolResult.toolCalls());
        result.put("validationWarnings", toolResult.validationWarnings());
        result.put("toolLimitReached", toolResult.toolLimitReached());
        result.put("knowledgeRefs", knowledgeRefs);
        result.put("knowledgeHit", !knowledgeRefs.isEmpty());
        result.put("knowledgeFallbackNotice", knowledgeRefs.isEmpty() ? "未命中知识库，当前回答来自规则或模拟提供者。" : "");
        return result;
    }

    private Map<String, Object> providerFailureResult(AIConversationEntity conversation,
                                                      AiProvider provider,
                                                      String scene,
                                                      long started,
                                                      Exception ex,
                                                      List<Map<String, Object>> knowledgeRefs) {
        Map<String, Object> failure = providerFailurePayload(provider, ex);
        int latencyMs = (int) (System.currentTimeMillis() - started);
        messageService.saveMessage(conversation.getId(), "ASSISTANT", String.valueOf(failure.get("message")),
                null, 0, latencyMs, String.valueOf(failure.get("errorCode")), "FAILED");
        conversationService.touchConversation(conversation, provider.providerName(), failure.get("model"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("conversationNo", conversation.getConversationNo());
        result.put("scene", scene);
        result.put("provider", failure.get("provider"));
        result.put("model", failure.get("model"));
        result.put("reply", "");
        result.put("failed", true);
        result.put("errorCode", failure.get("errorCode"));
        result.put("statusCode", failure.get("statusCode"));
        result.put("message", failure.get("message"));
        result.put("requestId", failure.get("requestId"));
        result.put("latencyMs", latencyMs);
        result.put("toolCalls", List.of());
        result.put("validationWarnings", List.of());
        result.put("toolLimitReached", false);
        result.put("knowledgeRefs", knowledgeRefs);
        result.put("knowledgeHit", !knowledgeRefs.isEmpty());
        result.put("knowledgeFallbackNotice", "");
        return result;
    }

    Map<String, Object> providerFailurePayload(AiProvider provider, Exception ex) {
        Map<String, Object> failure = new LinkedHashMap<>();
        failure.put("errorCode", "DEEPSEEK_FAILED");
        failure.put("provider", provider.providerName());
        failure.put("model", provider.providerName());
        failure.put("statusCode", null);
        failure.put("message", deepSeekFailureMessage());
        failure.put("requestId", "ai-" + System.currentTimeMillis());
        if (ex instanceof AiProviderCallException aiEx) {
            failure.put("errorCode", aiEx.getErrorCode());
            failure.put("provider", aiEx.getProvider());
            failure.put("model", aiEx.getModel());
            failure.put("statusCode", aiEx.getStatusCode());
            failure.put("message", aiEx.getSafeMessage());
        }
        return failure;
    }

    String deepSeekFailureMessage() {
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

    private AiProviderResponse fallbackToMock(String scene,
                                              String userMessage,
                                              Map<String, Object> context,
                                              String reason) {
        AiProvider mock = providerResolver.resolveProvider("mock", scene);
        try {
            AiProviderResponse fallback = mock.chat(scene, userMessage, context);
            Map<String, Object> metadata = new HashMap<>(fallback.metadata());
            metadata.put("fallbackReason", reason);
            return new AiProviderResponse(fallback.reply(), fallback.scene(), fallback.provider(), fallback.model(),
                    fallback.confidence(), fallback.tokens(), fallback.success(), fallback.errorCode(),
                    fallback.errorMessage(), fallback.usage(), fallback.toolCalls(), fallback.metrics(),
                    fallback.suggestions(), metadata);
        } catch (Exception ignored) {
            return AiProviderResponse.builder(scene, "system")
                    .reply("系统繁忙，请稍后重试。")
                    .confidence(0.1)
                    .build();
        }
    }
}
