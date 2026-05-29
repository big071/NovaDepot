package com.novadepot.backend.modules.ai;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final AiChatService chatService;
    private final AiStreamService streamService;
    private final AiConversationService conversationService;
    private final AiMessageService messageService;
    private final AiUsageLogService usageLogService;

    public AiService(AiChatService chatService,
                     AiStreamService streamService,
                     AiConversationService conversationService,
                     AiMessageService messageService,
                     AiUsageLogService usageLogService) {
        this.chatService = chatService;
        this.streamService = streamService;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.usageLogService = usageLogService;
    }

    public Map<String, Object> chat(AiChatRequest request) {
        return chatService.chat(request);
    }

    public SseEmitter streamChat(AiChatRequest request, String requestId) {
        return streamService.streamChat(request, requestId);
    }

    public void stopStream(String requestId) {
        streamService.stopStream(requestId);
    }

    public List<Map<String, Object>> conversations() {
        return conversationService.conversations();
    }

    public Map<String, Object> createConversation(String scene) {
        return conversationService.createConversation(scene);
    }

    public Map<String, Object> archiveConversation(Long conversationId) {
        return conversationService.archiveConversation(conversationId);
    }

    public List<Map<String, Object>> conversationMessages(Long conversationId) {
        return messageService.conversationMessages(conversationId);
    }

    public List<Map<String, Object>> conversationMessagesByNo(String conversationNo) {
        return messageService.conversationMessagesByNo(conversationNo);
    }

    public List<Map<String, Object>> usageLogs(Long conversationId, int limit) {
        return usageLogService.usageLogs(conversationId, limit);
    }
}
