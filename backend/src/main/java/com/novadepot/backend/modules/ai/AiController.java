package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@Validated
public class AiController {
    private final AiService service;

    public AiController(AiService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    @RequirePermission("AI_CHAT")
    public ApiResponse<Map<String, Object>> chat(@Valid @RequestBody AiChatRequest req) {
        return ApiResponse.success(service.chat(req), MDC.get("traceId"));
    }

    @PostMapping("/chat/stream")
    @RequirePermission("AI_CHAT")
    public SseEmitter streamChat(@Valid @RequestBody AiChatRequest req,
                                 @RequestParam(required = false) String requestId) {
        String safeRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        return service.streamChat(req, safeRequestId);
    }

    @PostMapping("/chat/stream/{requestId}/stop")
    @RequirePermission("AI_CHAT")
    public ApiResponse<Map<String, Object>> stopStream(@PathVariable String requestId) {
        service.stopStream(requestId);
        return ApiResponse.success(Map.of("requestId", requestId, "stopped", true), MDC.get("traceId"));
    }

    @PostMapping("/conversations")
    @RequirePermission("AI_CHAT")
    public ApiResponse<Map<String, Object>> createConversation(@RequestParam(required = false) String scene) {
        return ApiResponse.success(service.createConversation(scene), MDC.get("traceId"));
    }

    @PostMapping("/conversations/{id}/archive")
    @RequirePermission("AI_CHAT")
    public ApiResponse<Map<String, Object>> archiveConversation(@PathVariable Long id) {
        return ApiResponse.success(service.archiveConversation(id), MDC.get("traceId"));
    }

    @GetMapping("/conversations")
    @RequirePermission("AI_CHAT")
    public ApiResponse<List<Map<String, Object>>> conversations() {
        return ApiResponse.success(service.conversations(), MDC.get("traceId"));
    }

    @GetMapping("/conversations/{id}/messages")
    @RequirePermission("AI_CHAT")
    public ApiResponse<List<Map<String, Object>>> conversationMessages(@PathVariable Long id) {
        return ApiResponse.success(service.conversationMessages(id), MDC.get("traceId"));
    }

    @GetMapping("/conversations/by-no/{conversationNo}/messages")
    @RequirePermission("AI_CHAT")
    public ApiResponse<List<Map<String, Object>>> conversationMessagesByNo(@PathVariable String conversationNo) {
        return ApiResponse.success(service.conversationMessagesByNo(conversationNo), MDC.get("traceId"));
    }
}
