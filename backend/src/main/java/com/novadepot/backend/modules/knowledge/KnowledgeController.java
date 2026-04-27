package com.novadepot.backend.modules.knowledge;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    @GetMapping("/faqs")
    @RequirePermission("KNOWLEDGE_READ")
    public ApiResponse<List<Map<String, Object>>> faqs(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String scene,
                                                        @RequestParam(required = false) String status) {
        return ApiResponse.success(service.listFaqs(keyword, scene, status), MDC.get("traceId"));
    }

    @PostMapping("/faqs")
    @RequirePermission("KNOWLEDGE_DRAFT_WRITE")
    public ApiResponse<Map<String, Object>> createFaq(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.createFaq(body), MDC.get("traceId"));
    }

    @PutMapping("/faqs/{id}")
    @RequirePermission("KNOWLEDGE_DRAFT_WRITE")
    public ApiResponse<Map<String, Object>> updateFaq(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.updateFaq(id, body), MDC.get("traceId"));
    }

    @PostMapping("/faqs/{id}/confirm")
    @RequirePermission("KNOWLEDGE_CONFIRM")
    public ApiResponse<Map<String, Object>> confirmFaq(@PathVariable Long id) {
        return ApiResponse.success(service.confirmFaq(id), MDC.get("traceId"));
    }

    @PostMapping("/faqs/{id}/enable")
    @RequirePermission("KNOWLEDGE_CONFIRM")
    public ApiResponse<Map<String, Object>> enableFaq(@PathVariable Long id) {
        return ApiResponse.success(service.switchFaq(id, true), MDC.get("traceId"));
    }

    @PostMapping("/faqs/{id}/disable")
    @RequirePermission("KNOWLEDGE_CONFIRM")
    public ApiResponse<Map<String, Object>> disableFaq(@PathVariable Long id) {
        return ApiResponse.success(service.switchFaq(id, false), MDC.get("traceId"));
    }

    @GetMapping("/sops")
    @RequirePermission("KNOWLEDGE_READ")
    public ApiResponse<List<Map<String, Object>>> sops(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String scene,
                                                        @RequestParam(required = false) String status) {
        return ApiResponse.success(service.listSops(keyword, scene, status), MDC.get("traceId"));
    }

    @PostMapping("/sops")
    @RequirePermission("KNOWLEDGE_DRAFT_WRITE")
    public ApiResponse<Map<String, Object>> createSop(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.createSop(body), MDC.get("traceId"));
    }

    @PutMapping("/sops/{id}")
    @RequirePermission("KNOWLEDGE_DRAFT_WRITE")
    public ApiResponse<Map<String, Object>> updateSop(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.updateSop(id, body), MDC.get("traceId"));
    }

    @PostMapping("/sops/{id}/confirm")
    @RequirePermission("KNOWLEDGE_CONFIRM")
    public ApiResponse<Map<String, Object>> confirmSop(@PathVariable Long id) {
        return ApiResponse.success(service.confirmSop(id), MDC.get("traceId"));
    }

    @PostMapping("/sops/{id}/enable")
    @RequirePermission("KNOWLEDGE_CONFIRM")
    public ApiResponse<Map<String, Object>> enableSop(@PathVariable Long id) {
        return ApiResponse.success(service.switchSop(id, true), MDC.get("traceId"));
    }

    @PostMapping("/sops/{id}/disable")
    @RequirePermission("KNOWLEDGE_CONFIRM")
    public ApiResponse<Map<String, Object>> disableSop(@PathVariable Long id) {
        return ApiResponse.success(service.switchSop(id, false), MDC.get("traceId"));
    }

    @GetMapping("/rules")
    @RequirePermission("KNOWLEDGE_READ")
    public ApiResponse<List<Map<String, Object>>> rules(@RequestParam(required = false) String scene) {
        return ApiResponse.success(service.listRules(scene), MDC.get("traceId"));
    }

    @PutMapping("/rules/{configKey}")
    @RequirePermission("RULE_CONFIG_UPDATE")
    public ApiResponse<Map<String, Object>> updateRule(@PathVariable String configKey, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.updateRule(configKey, body), MDC.get("traceId"));
    }

    @PostMapping("/drafts/from-ticket/{ticketId}/faq")
    @RequirePermission("KNOWLEDGE_DRAFT_WRITE")
    public ApiResponse<Map<String, Object>> draftFaqFromTicket(@PathVariable Long ticketId) {
        return ApiResponse.success(service.draftFaqFromTicket(ticketId), MDC.get("traceId"));
    }

    @PostMapping("/drafts/from-ticket/{ticketId}/sop")
    @RequirePermission("KNOWLEDGE_DRAFT_WRITE")
    public ApiResponse<Map<String, Object>> draftSopFromTicket(@PathVariable Long ticketId) {
        return ApiResponse.success(service.draftSopFromTicket(ticketId), MDC.get("traceId"));
    }
}
