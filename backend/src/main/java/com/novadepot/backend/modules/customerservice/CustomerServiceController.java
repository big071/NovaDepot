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

    @GetMapping("/sessions/{id}/ai-suggestions")
    @RequirePermission("CS_SESSION_READ")
    public ApiResponse<Map<String, Object>> aiSuggestions(@PathVariable Long id) {
        return ApiResponse.success(service.aiSuggestions(id), MDC.get("traceId"));
    }

    @PostMapping("/sessions/{id}/messages")
    @RequirePermission("CS_MESSAGE_SEND")
    public ApiResponse<Map<String, Object>> sendMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String content = String.valueOf(body.getOrDefault("content", ""));
        String msgType = String.valueOf(body.getOrDefault("msgType", "TEXT"));
        Boolean sendByAi = body.get("sendByAi") instanceof Boolean b ? b : Boolean.FALSE;
        String senderType = String.valueOf(body.getOrDefault("senderType", ""));
        Boolean autoReply = body.get("autoReply") instanceof Boolean b ? b : Boolean.TRUE;
        return ApiResponse.success(service.sendMessage(id, content, msgType, sendByAi, senderType, autoReply), MDC.get("traceId"));
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

    @GetMapping("/tickets")
    @RequirePermission("CS_TICKET_READ")
    public ApiResponse<Map<String, Object>> tickets(@RequestParam(required = false) Long sessionId,
                                                     @RequestParam(defaultValue = "1") Integer pageNo,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(service.tickets(sessionId, pageNo, pageSize), MDC.get("traceId"));
    }

    @PutMapping("/tickets/{id}/status")
    @RequirePermission("CS_TICKET_UPDATE")
    public ApiResponse<Map<String, Object>> updateTicketStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = String.valueOf(body.getOrDefault("status", ""));
        String note = String.valueOf(body.getOrDefault("note", body.getOrDefault("remark", body.getOrDefault("closeReason", ""))));
        return ApiResponse.success(service.updateTicketStatus(id, status, note), MDC.get("traceId"));
    }

    @PutMapping("/tickets/{id}/owner")
    @RequirePermission("CS_TICKET_ASSIGN")
    public ApiResponse<Map<String, Object>> updateTicketOwner(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long assigneeUserId = body.get("assigneeUserId") instanceof Number n ? n.longValue() : null;
        return ApiResponse.success(service.updateTicketOwner(id, assigneeUserId), MDC.get("traceId"));
    }

    @PutMapping("/tickets/{id}/remark")
    @RequirePermission("CS_TICKET_REMARK")
    public ApiResponse<Map<String, Object>> updateTicketRemark(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String remark = String.valueOf(body.getOrDefault("remark", ""));
        return ApiResponse.success(service.updateTicketRemark(id, remark), MDC.get("traceId"));
    }

    @GetMapping("/tickets/{id}/timeline")
    @RequirePermission("CS_TICKET_READ")
    public ApiResponse<Map<String, Object>> ticketTimeline(@PathVariable Long id) {
        return ApiResponse.success(service.ticketTimeline(id), MDC.get("traceId"));
    }

    @GetMapping("/faq")
    @RequirePermission("CS_FAQ_READ")
    public ApiResponse<List<Map<String, Object>>> faq(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String scene) {
        return ApiResponse.success(service.faq(keyword, scene), MDC.get("traceId"));
    }

    @PutMapping("/faq/{id}")
    @RequirePermission("CS_FAQ_UPDATE")
    public ApiResponse<Map<String, Object>> updateFaq(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(
                service.updateFaq(id,
                        String.valueOf(body.getOrDefault("question", "")),
                        String.valueOf(body.getOrDefault("answer", "")),
                        String.valueOf(body.getOrDefault("scene", ""))),
                MDC.get("traceId")
        );
    }
}
