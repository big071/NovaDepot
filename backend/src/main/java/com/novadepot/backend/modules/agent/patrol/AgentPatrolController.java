package com.novadepot.backend.modules.agent.patrol;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent/patrol")
public class AgentPatrolController {
    private final AgentPatrolService service;
    @Value("${app.agent.patrol.order-overdue-hours:24}")
    private int orderOverdueHours;
    @Value("${app.agent.patrol.ticket-overdue-hours:4}")
    private int ticketOverdueHours;

    public AgentPatrolController(AgentPatrolService service) {
        this.service = service;
    }

    @PostMapping("/run")
    @RequirePermission("AGENT_PATROL_RUN")
    public ApiResponse<Map<String, Object>> run(@RequestParam String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        Map<String, Object> result = switch (normalized) {
            case "LOW_STOCK_PATROL", "LOW_STOCK" -> service.runLowStockPatrol();
            case "ORDER_OVERDUE_PATROL", "ORDER_OVERDUE" -> service.runOrderOverduePatrol(orderOverdueHours);
            case "TICKET_OVERDUE_PATROL", "TICKET_OVERDUE" -> service.runTicketOverduePatrol(ticketOverdueHours);
            default -> Map.of("error", "不支持的巡检类型");
        };
        return ApiResponse.success(result, MDC.get("traceId"));
    }
}
