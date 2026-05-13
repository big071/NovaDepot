package com.novadepot.backend.modules.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.config.AiProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiFunctionCallingOrchestrator {
    public static final int MAX_TOOL_CALLS = 5;

    private final AiProperties aiProperties;
    private final AiToolRegistry registry;
    private final AiToolExecutor executor;
    private final ObjectMapper objectMapper;

    public AiFunctionCallingOrchestrator(AiProperties aiProperties,
                                         AiToolRegistry registry,
                                         AiToolExecutor executor,
                                         ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.registry = registry;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    public void prepareContext(Map<String, Object> context) {
        if (!aiProperties.isToolsEnabled()) {
            return;
        }
        context.put("aiTools", registry.openAiToolsForCurrentUser());
        context.put("toolChoice", "auto");
    }

    @SuppressWarnings("unchecked")
    public AiFunctionCallingResult run(String userMessage,
                                       Map<String, Object> context,
                                       Map<String, Object> providerResp,
                                       Long conversationId,
                                       Long messageId,
                                       String requestId) {
        if (!aiProperties.isToolsEnabled()) {
            return AiFunctionCallingResult.empty();
        }
        List<AiToolCall> calls = new ArrayList<>();
        Object providerCalls = providerResp == null ? null : providerResp.get("toolCalls");
        if (providerCalls instanceof List<?> rawCalls) {
            for (Object item : rawCalls) {
                if (item instanceof Map<?, ?> map) {
                    Object name = map.get("name");
                    Object args = map.get("arguments");
                    if (name != null) {
                        calls.add(new AiToolCall(String.valueOf(name), args == null ? "{}" : String.valueOf(args)));
                    }
                }
            }
        }
        if (calls.isEmpty()) {
            calls.addAll(mockCalls(userMessage));
        }
        if (calls.isEmpty()) {
            return AiFunctionCallingResult.empty();
        }

        boolean limitReached = calls.size() > MAX_TOOL_CALLS;
        List<Map<String, Object>> toolResults = new ArrayList<>();
        for (AiToolCall call : calls.stream().limit(MAX_TOOL_CALLS).toList()) {
            AiToolResult result = executor.execute(call.toolName(), call.argumentsJson(), conversationId, messageId, requestId);
            toolResults.add(result.toMap());
        }
        List<String> warnings = validationWarnings(toolResults);
        String reply = composeReply(toolResults, limitReached, warnings);
        return new AiFunctionCallingResult(reply, toolResults, warnings, true, limitReached);
    }

    private List<AiToolCall> mockCalls(String message) {
        String text = message == null ? "" : message.toLowerCase();
        List<AiToolCall> calls = new ArrayList<>();
        if (containsAny(text, "日报", "今日", "运营", "概览", "today", "daily")) {
            calls.add(call("get_daily_report", Map.of("date", LocalDate.now().toString())));
        }
        if (containsAny(text, "库存", "低库存", "sku", "补货", "inventory")) {
            calls.add(call("get_inventory_stats", Map.of()));
            calls.add(call("query_inventory", Map.of("lowStock", text.contains("低库存"), "limit", 10)));
        }
        if (containsAny(text, "入库", "inbound")) {
            calls.add(call("query_inbound", Map.of("limit", 10)));
        }
        if (containsAny(text, "出库", "outbound")) {
            calls.add(call("query_outbound", Map.of("limit", 10)));
        }
        if (containsAny(text, "采购", "po-", "purchase")) {
            calls.add(call("query_purchase", Map.of("limit", 10)));
        }
        if (containsAny(text, "销售", "so-", "sale")) {
            calls.add(call("query_sale", Map.of("limit", 10)));
        }
        if (containsAny(text, "工单", "客服", "ticket")) {
            calls.add(call("query_tickets", Map.of("limit", 10)));
        }
        if (containsAny(text, "产品", "商品", "product")) {
            calls.add(call("query_product", Map.of("limit", 10)));
        }
        if (containsAny(text, "往来", "客户", "供应商", "partner")) {
            calls.add(call("query_partner", Map.of("limit", 10)));
        }
        return calls;
    }

    private AiToolCall call(String name, Map<String, Object> args) {
        try {
            return new AiToolCall(name, objectMapper.writeValueAsString(args));
        } catch (Exception ignored) {
            return new AiToolCall(name, "{}");
        }
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private List<String> validationWarnings(List<Map<String, Object>> toolResults) {
        if (toolResults.stream().allMatch(result -> Boolean.TRUE.equals(result.get("empty")))) {
            return List.of("未查询到相关数据，最终回答不得编造数字、单号或状态。");
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private String composeReply(List<Map<String, Object>> toolResults, boolean limitReached, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        if (limitReached) {
            sb.append("已达到本轮工具调用上限，请基于已有结果查看，或缩小查询范围后继续提问。\n");
        }
        if (!warnings.isEmpty()) {
            sb.append("未查询到相关数据。\n");
            return sb.toString().trim();
        }
        sb.append("已基于系统只读工具查询到以下结果：\n");
        for (Map<String, Object> result : toolResults) {
            sb.append("- ").append(result.getOrDefault("displayName", result.get("toolName"))).append("：")
                    .append(result.getOrDefault("summary", "已完成查询")).append("\n");
            Object sources = result.get("sources");
            if (sources instanceof List<?> list && !list.isEmpty()) {
                sb.append("  来源：");
                List<String> labels = new ArrayList<>();
                for (Object item : list.stream().limit(3).toList()) {
                    if (item instanceof Map<?, ?> source) {
                        Object bizNo = source.get("bizNo");
                        Object status = source.get("status");
                        Object quantity = source.get("quantity");
                        List<String> parts = new ArrayList<>();
                        if (bizNo != null) parts.add(String.valueOf(bizNo));
                        if (status != null) parts.add("状态 " + status);
                        if (quantity != null) parts.add("数量 " + quantity);
                        if (!parts.isEmpty()) labels.add(String.join(" / ", parts));
                    }
                }
                sb.append(String.join("；", labels)).append("\n");
            }
        }
        sb.append("以上数字、单号和状态均来自工具查询结果。");
        return sb.toString().trim();
    }
}
