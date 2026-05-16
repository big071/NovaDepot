package com.novadepot.backend.modules.ai.tools;

import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.repository.AuthQueryMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AiToolRegistry {
    private final AuthQueryMapper authQueryMapper;
    private final Map<String, AiToolDefinition> definitions = new LinkedHashMap<>();

    public AiToolRegistry(AuthQueryMapper authQueryMapper) {
        this.authQueryMapper = authQueryMapper;
        register("query_inventory", "查询库存", "查询当前库存，可按产品、SKU、仓库或低库存过滤", List.of("INVENTORY_READ"),
                props("productName", "string", "sku", "string", "warehouseId", "integer", "lowStock", "boolean", "limit", "integer"));
        register("query_inbound", "查询入库单", "查询入库单列表，可按状态、日期、来源单号或往来单位过滤", List.of("INBOUND_READ"),
                props("status", "string", "dateFrom", "string", "dateTo", "string", "partnerName", "string", "sourceNo", "string", "limit", "integer"));
        register("query_outbound", "查询出库单", "查询出库单列表，可按状态、日期、来源单号或往来单位过滤", List.of("OUTBOUND_READ"),
                props("status", "string", "dateFrom", "string", "dateTo", "string", "partnerName", "string", "sourceNo", "string", "limit", "integer"));
        register("query_purchase", "查询采购单", "查询采购单列表，可按状态、日期、单号或供应商过滤", List.of("PURCHASE_READ"),
                props("status", "string", "dateFrom", "string", "dateTo", "string", "partnerName", "string", "orderNo", "string", "limit", "integer"));
        register("query_sale", "查询销售单", "查询销售单列表，可按状态、日期、单号或客户过滤", List.of("SALES_READ"),
                props("status", "string", "dateFrom", "string", "dateTo", "string", "partnerName", "string", "orderNo", "string", "limit", "integer"));
        register("query_tickets", "查询工单", "查询客服工单，可按状态、处理人、关键词或日期过滤", List.of("CS_SESSION_READ"),
                props("status", "string", "assignee", "string", "keyword", "string", "dateFrom", "string", "dateTo", "string", "limit", "integer"));
        register("query_product", "查询产品", "查询产品信息，可按名称、SKU、分类或状态过滤", List.of("PRODUCT_READ"),
                props("name", "string", "sku", "string", "category", "string", "enabled", "boolean", "limit", "integer"));
        register("query_partner", "查询往来单位", "查询供应商、客户或兼具两者的往来单位", List.of("PARTNER_READ"),
                props("name", "string", "type", "string", "enabled", "boolean", "limit", "integer"));
        register("get_inventory_stats", "库存统计", "获取库存概览、低库存和SKU统计", List.of("INVENTORY_READ"),
                props("warehouseId", "integer", "lowStockOnly", "boolean"));
        register("get_daily_report", "今日运营概览", "获取指定日期的入库、出库、采购、销售、工单运营概览", List.of(),
                props("date", "string"));
    }

    public Optional<AiToolDefinition> find(String name) {
        return Optional.ofNullable(definitions.get(name));
    }

    public List<Map<String, Object>> openAiToolsForCurrentUser() {
        List<String> permissions = currentPermissions();
        return definitions.values().stream()
                .filter(def -> canUse(def, permissions))
                .map(AiToolDefinition::toOpenAiTool)
                .toList();
    }

    public boolean canUse(AiToolDefinition definition, List<String> permissions) {
        if (definition.requiredPermissions().isEmpty()) {
            return true;
        }
        return definition.requiredPermissions().stream().anyMatch(permissions::contains);
    }

    public List<String> currentPermissions() {
        return authQueryMapper.findPermissions(RequestContext.tenantId(), RequestContext.userId());
    }

    private void register(String name, String displayName, String description, List<String> permissions, Map<String, Object> parameters) {
        definitions.put(name, new AiToolDefinition(name, displayName, description, permissions, parameters));
    }

    private Map<String, Object> props(Object... kv) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            String key = String.valueOf(kv[i]);
            String type = String.valueOf(kv[i + 1]);
            properties.put(key, Map.of("type", type));
        }
        return Map.of(
                "type", "object",
                "properties", properties,
                "additionalProperties", false
        );
    }
}
