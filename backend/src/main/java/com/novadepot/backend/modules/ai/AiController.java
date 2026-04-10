package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/conversations")
    @RequirePermission("AI_CHAT")
    public ApiResponse<List<Map<String, Object>>> conversations() {
        return ApiResponse.success(service.conversations(), MDC.get("traceId"));
    }
}
