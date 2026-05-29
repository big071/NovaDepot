package com.novadepot.backend.modules.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.config.AiProperties;
import com.novadepot.backend.modules.ai.provider.AiProviderResponse;
import com.novadepot.backend.modules.ai.provider.AiProviderResponseMapper;
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
    private final AiProviderResponseMapper providerResponseMapper;

    public AiFunctionCallingOrchestrator(AiProperties aiProperties,
                                         AiToolRegistry registry,
                                         AiToolExecutor executor,
                                         ObjectMapper objectMapper,
                                         AiProviderResponseMapper providerResponseMapper) {
        this.aiProperties = aiProperties;
        this.registry = registry;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.providerResponseMapper = providerResponseMapper;
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
                                       AiProviderResponse providerResp,
                                       Long conversationId,
                                       Long messageId,
                                       String requestId) {
        if (!aiProperties.isToolsEnabled()) {
            return AiFunctionCallingResult.empty();
        }
        List<AiToolCall> calls = new ArrayList<>();
        if (providerResp != null) {
            calls.addAll(providerResponseMapper.toolCallsToFunctionCalls(providerResp));
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
            sb.append("**当前结论**\n");
            sb.append("已达到本轮最多 ").append(MAX_TOOL_CALLS).append(" 次查询上限，我先基于已返回的数据给出结论。\n\n");
        }
        if (!warnings.isEmpty()) {
            sb.append("**当前结论**\n未查询到相关数据。\n\n");
            sb.append("**主要风险**\n- 暂无可验证数据，不能判断库存、金额、单号或状态。\n\n");
            sb.append("**建议动作**\n- 请补充商品、单号、状态或时间范围后重新查询。\n\n");
            sb.append("**数据依据**\n- 本轮只读查询没有返回业务数据。\n\n");
            sb.append("**下一步可执行操作**\n- 缩小查询条件后再次提问，例如提供 SKU、采购单号、出库单号或工单号。");
            return sb.toString().trim();
        }

        sb.append("**当前结论**\n");
        sb.append("我已查询系统业务数据，结果如下：");
        sb.append("\n");
        for (Map<String, Object> result : successfulResults(toolResults)) {
            sb.append("- ").append(result.getOrDefault("displayName", "业务数据")).append("：")
                    .append(result.getOrDefault("summary", "已完成查询")).append("\n");
        }

        sb.append("\n**主要风险**\n");
        List<String> risks = risks(toolResults);
        if (risks.isEmpty()) {
            sb.append("- 当前查询结果未发现明确高风险项，请结合业务阈值继续复核。\n");
        } else {
            for (String risk : risks) {
                sb.append("- ").append(risk).append("\n");
            }
        }

        sb.append("\n**建议动作**\n");
        for (Map<String, Object> result : successfulResults(toolResults)) {
            sb.append("- ").append(actionFor(result)).append("\n");
        }

        sb.append("\n**数据依据**\n");
        for (Map<String, Object> result : toolResults) {
            String name = String.valueOf(result.getOrDefault("displayName", "业务查询"));
            sb.append("- ").append(name).append("：")
                    .append(result.getOrDefault("summary", "已完成查询"));
            Object sources = result.get("sources");
            if (sources instanceof List<?> list && !list.isEmpty()) {
                sb.append("；来源：");
                List<String> labels = new ArrayList<>();
                for (Object item : list.stream().limit(3).toList()) {
                    if (item instanceof Map<?, ?> source) {
                        labels.add(sourceLabel(source));
                    }
                }
                sb.append(String.join("；", labels));
            }
            sb.append("\n");
        }

        sb.append("\n**下一步可执行操作**\n");
        sb.append("- 请业务人员按上述单号、SKU 或状态进入对应页面复核，涉及审核、发货、付款、改库存等写操作需人工确认后执行。\n");
        sb.append("- 以上数字、单号和状态均来自本轮只读查询结果。");
        return sb.toString().trim();
    }

    private List<Map<String, Object>> successfulResults(List<Map<String, Object>> toolResults) {
        return toolResults.stream()
                .filter(result -> !Boolean.FALSE.equals(result.get("success")))
                .filter(result -> !Boolean.TRUE.equals(result.get("empty")))
                .toList();
    }

    private List<String> risks(List<Map<String, Object>> toolResults) {
        List<String> risks = new ArrayList<>();
        for (Map<String, Object> result : successfulResults(toolResults)) {
            Object rows = result.get("rows");
            if (!(rows instanceof List<?> list)) {
                continue;
            }
            for (Object item : list.stream().limit(5).toList()) {
                if (item instanceof Map<?, ?> row) {
                    Object availableQty = row.get("availableQty");
                    Object status = row.get("status");
                    Object priority = row.get("priority");
                    if (availableQty != null) {
                        risks.add("高优先级：库存项 " + sourceLabel(row) + " 当前可用库存为 " + availableQty + "，请优先复核补货。");
                    } else if (priority != null) {
                        risks.add("中优先级：工单 " + sourceLabel(row) + " 优先级为 " + priority + "，请确认是否超时或需人工接管。");
                    } else if (status != null) {
                        risks.add("中优先级：单据 " + sourceLabel(row) + " 状态为 " + status + "，请按业务流程复核。");
                    }
                }
            }
        }
        return risks.stream().distinct().limit(5).toList();
    }

    private String actionFor(Map<String, Object> result) {
        String name = String.valueOf(result.getOrDefault("displayName", "业务数据"));
        if (name.contains("库存")) {
            return "对低库存或可用库存偏低的 SKU 建议先确认在途、锁定量和安全库存，再发起补货或调拨申请。";
        }
        if (name.contains("采购")) {
            return "对待处理采购单建议核对预计到货日期、供应商和金额，必要时催达到货确认。";
        }
        if (name.contains("出库")) {
            return "对待处理出库单建议核对库存占用、客户要求和发货时效，再安排拣货发运。";
        }
        if (name.contains("工单") || name.contains("客服")) {
            return "对高优先级或未关闭工单建议先查看客户诉求，再决定回复、升级或人工接管。";
        }
        return "请进入对应业务页面复核明细，确认后再执行写操作。";
    }

    private String sourceLabel(Map<?, ?> source) {
        List<String> parts = new ArrayList<>();
        Object bizNo = source.get("bizNo");
        Object sku = source.get("sku");
        Object productName = source.get("productName");
        Object name = source.get("name");
        Object status = source.get("status");
        Object availableQty = source.get("availableQty");
        Object quantity = source.get("quantity");
        Object amount = source.get("totalAmount");
        if (bizNo != null) parts.add(String.valueOf(bizNo));
        if (sku != null) parts.add("SKU " + sku);
        if (productName != null) parts.add(String.valueOf(productName));
        if (name != null) parts.add(String.valueOf(name));
        if (status != null) parts.add("状态 " + status);
        if (availableQty != null) parts.add("可用 " + availableQty);
        if (quantity != null) parts.add("数量 " + quantity);
        if (amount != null) parts.add("金额 " + amount);
        return parts.isEmpty() ? "-" : String.join(" / ", parts);
    }
}
