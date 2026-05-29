package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AIConversationEntity;
import com.novadepot.backend.model.entity.AIMessageEntity;
import com.novadepot.backend.modules.ai.provider.AiProvider;
import com.novadepot.backend.modules.ai.provider.AiProviderCallException;
import com.novadepot.backend.modules.ai.provider.AiProviderResponse;
import com.novadepot.backend.modules.ai.tools.AiFunctionCallingOrchestrator;
import com.novadepot.backend.modules.ai.tools.AiFunctionCallingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AiStreamService {
    private static final Logger log = LoggerFactory.getLogger(AiStreamService.class);

    private final AiProviderResolver providerResolver;
    private final AiPromptService promptService;
    private final AiUsageLogService usageLogService;
    private final AiConversationService conversationService;
    private final AiMessageService messageService;
    private final AiStreamRegistry streamRegistry;
    private final AiFunctionCallingOrchestrator functionCallingOrchestrator;
    private final AiAuditService auditService;
    private final boolean fallbackEnabled;

    public AiStreamService(AiProviderResolver providerResolver,
                           AiPromptService promptService,
                           AiUsageLogService usageLogService,
                           AiConversationService conversationService,
                           AiMessageService messageService,
                           AiStreamRegistry streamRegistry,
                           AiFunctionCallingOrchestrator functionCallingOrchestrator,
                           AiAuditService auditService,
                           @Value("${app.ai.fallback-enabled:false}") boolean fallbackEnabled) {
        this.providerResolver = providerResolver;
        this.promptService = promptService;
        this.usageLogService = usageLogService;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.streamRegistry = streamRegistry;
        this.functionCallingOrchestrator = functionCallingOrchestrator;
        this.auditService = auditService;
        this.fallbackEnabled = fallbackEnabled;
    }

    public void stopStream(String requestId) {
        streamRegistry.stop(requestId);
        auditService.writeAudit("AI_STREAM_STOP", null, requestId, null, Map.of("requestId", requestId == null ? "" : requestId));
    }

    public SseEmitter streamChat(AiChatRequest request, String requestId) {
        SseEmitter emitter = new SseEmitter(0L);
        Long tenantId = RequestContext.tenantId();
        Long userId = RequestContext.userId();
        CompletableFuture.runAsync(() -> {
            RequestContext.setTenantId(tenantId);
            RequestContext.setUserId(userId);
            try {
                doStreamChat(request, requestId, emitter);
            } finally {
                streamRegistry.clear(requestId);
                RequestContext.clear();
            }
        });
        return emitter;
    }

    private void doStreamChat(AiChatRequest request, String requestId, SseEmitter emitter) {
        String scene = promptService.normalizeScene(request.getScene());
        String userMessage = request.getMessage() == null ? "" : request.getMessage().trim();
        if (!StringUtils.hasText(userMessage)) {
            sendEvent(emitter, "error", Map.of("message", "请输入问题后再发送"));
            emitter.complete();
            return;
        }

        String resolvedProviderName = providerResolver.resolveProviderName(request.getProviderHint());
        AIConversationEntity conversation = conversationService.resolveConversation(request.getConversationId(), request.getConversationNo(), scene, resolvedProviderName);
        if ("ARCHIVED".equalsIgnoreCase(conversation.getStatus())) {
            sendEvent(emitter, "error", Map.of("message", "归档会话只读，请新建会话后继续提问。"));
            emitter.complete();
            return;
        }

        List<Map<String, String>> historyMessages = messageService.recentContextMessages(conversation.getId());
        messageService.saveMessage(conversation.getId(), "USER", userMessage, null, null, null, null, "COMPLETED");
        AIMessageEntity assistantMessage = messageService.saveMessage(conversation.getId(), "ASSISTANT", "", null, null, null, null, "STREAMING");

        Map<String, Object> context = new HashMap<>();
        context.put("conversationId", conversation.getId());
        context.put("tenantId", RequestContext.tenantId());
        context.put("userId", RequestContext.userId());
        context.put("scene", scene);
        context.put("historyMessages", historyMessages);
        functionCallingOrchestrator.prepareContext(context);
        context.put("stream", true);

        sendEvent(emitter, "meta", Map.of(
                "requestId", requestId,
                "conversationId", conversation.getId(),
                "conversationNo", conversation.getConversationNo(),
                "messageId", assistantMessage.getId(),
                "status", "STREAMING"
        ));

        long started = System.currentTimeMillis();
        AiProvider provider = providerResolver.resolveProvider(resolvedProviderName, scene);
        String providerInput = "rule".equalsIgnoreCase(provider.providerName())
                ? userMessage
                : promptService.renderPrompt(scene, userMessage);
        AiProviderResponse providerResp;
        String fallbackFrom = null;
        try {
            providerResp = provider.chat(scene, providerInput, context);
        } catch (Exception ex) {
            fallbackFrom = provider.providerName();
            log.warn("AI stream provider failed, provider={}, scene={}, reason={}", provider.providerName(), scene, ex.getMessage());
            if (!fallbackEnabled) {
                Map<String, Object> failure = providerFailurePayload(provider, ex);
                messageService.updateMessage(assistantMessage, String.valueOf(failure.get("message")), null, null,
                        (int) (System.currentTimeMillis() - started), String.valueOf(failure.get("errorCode")), "FAILED");
                conversationService.touchConversation(conversation, provider.providerName(), failure.get("model"));
                sendEvent(emitter, "error", failure);
                emitter.complete();
                return;
            }
            usageLogService.saveDegradationUsageLog(context, scene, fallbackFrom, started, ex.getMessage());
            sendEvent(emitter, "status", Map.of("status", "FALLBACK", "fallbackFrom", fallbackFrom, "message", "DeepSeek不可用，已切换至规则/模拟降级。"));
            try {
                provider = providerResolver.resolveProvider("rule", scene);
                providerResp = provider.chat(scene, userMessage, context);
            } catch (Exception ruleEx) {
                providerResp = fallbackToMock(scene, userMessage, context, ruleEx.getMessage());
                provider = providerResolver.resolveProvider("mock", scene);
            }
        }

        AiFunctionCallingResult toolResult = functionCallingOrchestrator.run(
                userMessage, context, providerResp, conversation.getId(), assistantMessage.getId(), requestId);
        if (!providerResolver.isDeepSeekProvider(provider.providerName())) {
            usageLogService.saveManualProviderUsageLog(context, scene, provider.providerName(),
                    (int) (System.currentTimeMillis() - started), true, null, null);
        }
        for (Map<String, Object> toolCall : toolResult.toolCalls()) {
            sendEvent(emitter, "tool_start", Map.of(
                    "toolName", toolCall.getOrDefault("toolName", ""),
                    "displayName", toolCall.getOrDefault("displayName", ""),
                    "argumentsSummary", toolCall.getOrDefault("argumentsSummary", "")
            ));
            sendEvent(emitter, "tool_result", toolCall);
        }
        for (String warning : toolResult.validationWarnings()) {
            sendEvent(emitter, "validation_warning", Map.of("message", warning));
        }
        if (toolResult.toolLimitReached()) {
            sendEvent(emitter, "tool_limit", Map.of("message", "已达到本轮最多 5 次工具调用限制"));
        }

        String reply = providerResp.reply() == null ? "" : providerResp.reply();
        if (toolResult.usedTools()) {
            reply = toolResult.reply();
        }
        StringBuilder streamed = new StringBuilder();
        boolean stopped = false;
        try {
            for (String chunk : chunks(reply, 8)) {
                if (streamRegistry.isStopped(requestId)) {
                    stopped = true;
                    break;
                }
                streamed.append(chunk);
                sendEvent(emitter, "token", Map.of("content", chunk));
                sleepQuietly(25);
            }
            int latencyMs = (int) (System.currentTimeMillis() - started);
            String finalContent = stopped ? streamed.toString() : reply;
            messageService.updateMessage(assistantMessage, finalContent, providerResp.confidence(),
                    providerResp.tokens(), latencyMs, fallbackFrom == null ? null : "AI_PROVIDER_FALLBACK",
                    stopped ? "STOPPED" : "COMPLETED");
            conversationService.touchConversation(conversation, provider.providerName(), providerResp.model());
            if (stopped) {
                sendEvent(emitter, "status", Map.of("status", "STOPPED"));
            }
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("conversationId", conversation.getId());
            done.put("conversationNo", conversation.getConversationNo());
            done.put("provider", provider.providerName());
            done.put("status", stopped ? "STOPPED" : "COMPLETED");
            done.put("latencyMs", latencyMs);
            if (fallbackFrom != null) {
                done.put("fallbackFrom", fallbackFrom);
            }
            done.put("toolCalls", toolResult.toolCalls());
            done.put("validationWarnings", toolResult.validationWarnings());
            done.put("toolLimitReached", toolResult.toolLimitReached());
            sendEvent(emitter, "done", done);
            emitter.complete();
        } catch (Exception ex) {
            messageService.updateMessage(assistantMessage, streamed.toString(), null, null,
                    (int) (System.currentTimeMillis() - started), "AI_STREAM_FAILED", "FAILED");
            sendEvent(emitter, "error", Map.of("message", "流式输出失败，前端可切换为普通回复。"));
            emitter.complete();
        }
    }

    private Map<String, Object> providerFailurePayload(AiProvider provider, Exception ex) {
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

    private List<String> chunks(String text, int size) {
        if (text == null || text.isEmpty()) {
            return List.of("");
        }
        List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < text.length(); i += size) {
            result.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return result;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send SSE event", ex);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
