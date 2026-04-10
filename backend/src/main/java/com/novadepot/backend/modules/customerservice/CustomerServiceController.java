package com.novadepot.backend.modules.customerservice;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer-service")
public class CustomerServiceController {
    private final CustomerServiceService service;

    public CustomerServiceController(CustomerServiceService service) {
        this.service = service;
    }

    @GetMapping("/sessions")
    @RequirePermission("CS_SESSION_READ")
    public ApiResponse<List<Map<String, Object>>> sessions() {
        return ApiResponse.success(service.sessions(), MDC.get("traceId"));
    }

    @GetMapping("/sessions/{id}/messages")
    @RequirePermission("CS_SESSION_READ")
    public ApiResponse<List<Map<String, Object>>> messages(@PathVariable Long id) {
        return ApiResponse.success(service.messages(id), MDC.get("traceId"));
    }

    @PostMapping("/sessions/{id}/messages")
    @RequirePermission("CS_MESSAGE_SEND")
    public ApiResponse<Map<String, Object>> sendMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String content = String.valueOf(body.getOrDefault("content", ""));
        String msgType = String.valueOf(body.getOrDefault("msgType", "TEXT"));
        Boolean sendByAi = body.get("sendByAi") instanceof Boolean b ? b : Boolean.FALSE;
        return ApiResponse.success(service.sendMessage(id, content, msgType, sendByAi), MDC.get("traceId"));
    }

    @PostMapping("/sessions/{id}/actions/transfer-human")
    @RequirePermission("CS_TRANSFER_HUMAN")
    public ApiResponse<Map<String, Object>> transferHuman(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        return ApiResponse.success(service.transferHuman(id, body.getOrDefault("targetUserId", 1L)), MDC.get("traceId"));
    }

    @PostMapping("/tickets")
    @RequirePermission("CS_TICKET_CREATE")
    public ApiResponse<Map<String, Object>> createTicket(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("sessionId") instanceof Number n ? n.longValue() : 0L;
        String priority = String.valueOf(body.getOrDefault("priority", "MEDIUM"));
        String content = String.valueOf(body.getOrDefault("content", ""));
        return ApiResponse.success(service.createTicket(sessionId, priority, content), MDC.get("traceId"));
    }

    @GetMapping("/faq")
    @RequirePermission("CS_FAQ_READ")
    public ApiResponse<List<Map<String, Object>>> faq(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String scene) {
        return ApiResponse.success(service.faq(keyword, scene), MDC.get("traceId"));
    }
}
