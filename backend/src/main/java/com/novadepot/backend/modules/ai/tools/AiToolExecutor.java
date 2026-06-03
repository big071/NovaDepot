package com.novadepot.backend.modules.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.*;
import com.novadepot.backend.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class AiToolExecutor {
    private final AiToolRegistry registry;
    private final ObjectMapper objectMapper;
    private final AiToolCallLogMapper toolLogMapper;
    private final AuditLogMapper auditLogMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final CustomerServiceTicketMapper ticketMapper;
    private final PartnerMapper partnerMapper;

    public AiToolExecutor(AiToolRegistry registry,
                          ObjectMapper objectMapper,
                          AiToolCallLogMapper toolLogMapper,
                          AuditLogMapper auditLogMapper,
                          InventoryMapper inventoryMapper,
                          ProductMapper productMapper,
                          WarehouseMapper warehouseMapper,
                          InboundOrderMapper inboundOrderMapper,
                          OutboundOrderMapper outboundOrderMapper,
                          PurchaseOrderMapper purchaseOrderMapper,
                          SalesOrderMapper salesOrderMapper,
                          CustomerServiceTicketMapper ticketMapper,
                          PartnerMapper partnerMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.toolLogMapper = toolLogMapper;
        this.auditLogMapper = auditLogMapper;
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.ticketMapper = ticketMapper;
        this.partnerMapper = partnerMapper;
    }

    public AiToolResult execute(String toolName, String argumentsJson, Long conversationId, Long messageId, String requestId) {
        long started = System.currentTimeMillis();
        AiToolDefinition definition = registry.find(toolName).orElse(null);
        if (definition == null) {
            AiToolResult result = new AiToolResult();
            result.setToolName(toolName);
            result.setDisplayName(toolName);
            result.setArgumentsSummary(truncate(argumentsJson, 512));
            result.setSuccess(false);
            result.setPermissionResult("UNKNOWN_TOOL");
            result.setEmpty(true);
            result.setSummary("未知工具：" + toolName);
            result.setErrorCode("AI_TOOL_UNKNOWN");
            result.setDurationMs((int) (System.currentTimeMillis() - started));
            writeLogs(result, conversationId, messageId, requestId);
            return result;
        }

        String argsSummary = truncate(argumentsJson, 512);
        List<String> permissions = registry.currentPermissions();
        if (!registry.canUse(definition, permissions)) {
            AiToolResult denied = AiToolResult.denied(definition, argsSummary);
            denied.setDurationMs((int) (System.currentTimeMillis() - started));
            writeLogs(denied, conversationId, messageId, requestId);
            return denied;
        }

        Map<String, Object> args = parseArgs(argumentsJson);
        AiToolResult result;
        try {
            result = switch (toolName) {
                case "query_inventory" -> queryInventory(args);
                case "query_inbound" -> queryInbound(args);
                case "query_outbound" -> queryOutbound(args);
                case "query_purchase" -> queryPurchase(args);
                case "query_sale" -> querySale(args);
                case "query_tickets" -> queryTickets(args);
                case "query_product" -> queryProduct(args);
                case "query_partner" -> queryPartner(args);
                case "get_inventory_stats" -> getInventoryStats(args);
                case "get_daily_report" -> getDailyReport(args, permissions);
                default -> throw new IllegalArgumentException("Unsupported tool " + toolName);
            };
        } catch (Exception ex) {
            result = new AiToolResult();
            result.setToolName(toolName);
            result.setDisplayName(definition.displayName());
            result.setSuccess(false);
            result.setPermissionResult("ALLOWED");
            result.setEmpty(true);
            result.setSummary("工具查询失败");
            result.setErrorCode("AI_TOOL_FAILED");
            result.setErrorMessage(truncate(ex.getMessage(), 512));
        }
        result.setToolName(definition.name());
        result.setDisplayName(definition.displayName());
        result.setArgumentsSummary(argsSummary);
        result.setDurationMs((int) (System.currentTimeMillis() - started));
        if (result.getRows().isEmpty()) {
            result.setEmpty(true);
            if (!StringUtils.hasText(result.getSummary())) {
                result.setSummary("未查询到相关数据");
            }
        }
        writeLogs(result, conversationId, messageId, requestId);
        return result;
    }

    private AiToolResult queryInventory(Map<String, Object> args) {
        int limit = limit(args);
        Long tenantId = RequestContext.tenantId();
        Long productId = null;
        List<Long> productIds = productIds(args.get("productName"), args.get("sku"));
        LambdaQueryWrapper<InventoryEntity> q = new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, tenantId)
                .eq(readLong(args.get("warehouseId")) != null, InventoryEntity::getWarehouseId, readLong(args.get("warehouseId")))
                .in(!productIds.isEmpty(), InventoryEntity::getProductId, productIds)
                .le(Boolean.TRUE.equals(readBoolean(args.get("lowStock"))), InventoryEntity::getAvailableQty, BigDecimal.TEN)
                .orderByAsc(InventoryEntity::getAvailableQty);
        List<Map<String, Object>> rows = selectLimited(inventoryMapper, q, limit).stream().map(row -> {
            ProductEntity p = productMapper.selectById(row.getProductId());
            WarehouseEntity w = warehouseMapper.selectById(row.getWarehouseId());
            return map("sourceType", "inventory", "sourceId", row.getId(), "productName", p == null ? row.getProductId() : p.getProductName(),
                    "sku", p == null ? "" : p.getProductCode(), "warehouse", w == null ? row.getWarehouseId() : w.getWarehouseName(),
                    "availableQty", row.getAvailableQty(), "lockedQty", row.getLockedQty(), "inTransitQty", row.getInTransitQty());
        }).toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条库存记录");
    }

    private AiToolResult queryInbound(Map<String, Object> args) {
        LambdaQueryWrapper<InboundOrderEntity> q = new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(text(args, "status") != null, InboundOrderEntity::getStatus, text(args, "status"))
                .like(text(args, "sourceNo") != null, InboundOrderEntity::getSourceOrderNo, text(args, "sourceNo"))
                .ge(dateStart(args, "dateFrom") != null, InboundOrderEntity::getCreatedAt, dateStart(args, "dateFrom"))
                .lt(dateEnd(args, "dateTo") != null, InboundOrderEntity::getCreatedAt, dateEnd(args, "dateTo"))
                .orderByDesc(InboundOrderEntity::getCreatedAt);
        List<Map<String, Object>> rows = selectLimited(inboundOrderMapper, q, limit(args)).stream()
                .map(r -> map("sourceType", "inbound", "sourceId", r.getId(), "bizNo", r.getInboundNo(), "status", r.getStatus(),
                        "sourceNo", r.getSourceOrderNo(), "warehouseId", r.getWarehouseId(), "createdAt", r.getCreatedAt()))
                .toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条入库单");
    }

    private AiToolResult queryOutbound(Map<String, Object> args) {
        LambdaQueryWrapper<OutboundOrderEntity> q = new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(text(args, "status") != null, OutboundOrderEntity::getStatus, text(args, "status"))
                .like(text(args, "sourceNo") != null, OutboundOrderEntity::getSourceOrderNo, text(args, "sourceNo"))
                .ge(dateStart(args, "dateFrom") != null, OutboundOrderEntity::getCreatedAt, dateStart(args, "dateFrom"))
                .lt(dateEnd(args, "dateTo") != null, OutboundOrderEntity::getCreatedAt, dateEnd(args, "dateTo"))
                .orderByDesc(OutboundOrderEntity::getCreatedAt);
        List<Map<String, Object>> rows = selectLimited(outboundOrderMapper, q, limit(args)).stream()
                .map(r -> map("sourceType", "outbound", "sourceId", r.getId(), "bizNo", r.getOutboundNo(), "status", r.getStatus(),
                        "sourceNo", r.getSourceOrderNo(), "warehouseId", r.getWarehouseId(), "createdAt", r.getCreatedAt()))
                .toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条出库单");
    }

    private AiToolResult queryPurchase(Map<String, Object> args) {
        LambdaQueryWrapper<PurchaseOrderEntity> q = new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(text(args, "status") != null, PurchaseOrderEntity::getStatus, text(args, "status"))
                .like(text(args, "orderNo") != null, PurchaseOrderEntity::getPurchaseNo, text(args, "orderNo"))
                .in(partnerIds(args.get("partnerName")).size() > 0, PurchaseOrderEntity::getPartnerId, partnerIds(args.get("partnerName")))
                .ge(dateStart(args, "dateFrom") != null, PurchaseOrderEntity::getCreatedAt, dateStart(args, "dateFrom"))
                .lt(dateEnd(args, "dateTo") != null, PurchaseOrderEntity::getCreatedAt, dateEnd(args, "dateTo"))
                .orderByDesc(PurchaseOrderEntity::getCreatedAt);
        List<Map<String, Object>> rows = selectLimited(purchaseOrderMapper, q, limit(args)).stream()
                .map(r -> map("sourceType", "purchase", "sourceId", r.getId(), "bizNo", r.getPurchaseNo(), "status", r.getStatus(),
                        "totalAmount", r.getTotalAmount(), "expectedArrivalDate", r.getExpectedArrivalDate(), "createdAt", r.getCreatedAt()))
                .toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条采购单");
    }

    private AiToolResult querySale(Map<String, Object> args) {
        LambdaQueryWrapper<SalesOrderEntity> q = new LambdaQueryWrapper<SalesOrderEntity>()
                .eq(SalesOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(text(args, "status") != null, SalesOrderEntity::getStatus, text(args, "status"))
                .like(text(args, "orderNo") != null, SalesOrderEntity::getSalesNo, text(args, "orderNo"))
                .in(partnerIds(args.get("partnerName")).size() > 0, SalesOrderEntity::getPartnerId, partnerIds(args.get("partnerName")))
                .ge(dateStart(args, "dateFrom") != null, SalesOrderEntity::getCreatedAt, dateStart(args, "dateFrom"))
                .lt(dateEnd(args, "dateTo") != null, SalesOrderEntity::getCreatedAt, dateEnd(args, "dateTo"))
                .orderByDesc(SalesOrderEntity::getCreatedAt);
        List<Map<String, Object>> rows = selectLimited(salesOrderMapper, q, limit(args)).stream()
                .map(r -> map("sourceType", "sale", "sourceId", r.getId(), "bizNo", r.getSalesNo(), "status", r.getStatus(),
                        "totalAmount", r.getTotalAmount(), "deliveryDate", r.getDeliveryDate(), "createdAt", r.getCreatedAt()))
                .toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条销售单");
    }

    private AiToolResult queryTickets(Map<String, Object> args) {
        LambdaQueryWrapper<CustomerServiceTicketEntity> q = new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId())
                .eq(text(args, "status") != null, CustomerServiceTicketEntity::getStatus, text(args, "status"))
                .like(text(args, "keyword") != null, CustomerServiceTicketEntity::getContent, text(args, "keyword"))
                .ge(dateStart(args, "dateFrom") != null, CustomerServiceTicketEntity::getCreatedAt, dateStart(args, "dateFrom"))
                .lt(dateEnd(args, "dateTo") != null, CustomerServiceTicketEntity::getCreatedAt, dateEnd(args, "dateTo"))
                .orderByDesc(CustomerServiceTicketEntity::getCreatedAt);
        List<Map<String, Object>> rows = selectLimited(ticketMapper, q, limit(args)).stream()
                .map(r -> map("sourceType", "ticket", "sourceId", r.getId(), "bizNo", r.getTicketNo(), "status", r.getStatus(),
                        "priority", r.getPriority(), "content", truncate(r.getContent(), 80), "createdAt", r.getCreatedAt()))
                .toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条工单");
    }

    private AiToolResult queryProduct(Map<String, Object> args) {
        LambdaQueryWrapper<ProductEntity> q = new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .like(text(args, "name") != null, ProductEntity::getProductName, text(args, "name"))
                .like(text(args, "sku") != null, ProductEntity::getProductCode, text(args, "sku"))
                .eq(readBoolean(args.get("enabled")) != null, ProductEntity::getStatus, Boolean.TRUE.equals(readBoolean(args.get("enabled"))) ? "ACTIVE" : "INACTIVE")
                .orderByDesc(ProductEntity::getCreatedAt);
        List<Map<String, Object>> rows = selectLimited(productMapper, q, limit(args)).stream()
                .map(r -> map("sourceType", "product", "sourceId", r.getId(), "bizNo", r.getProductCode(), "name", r.getProductName(),
                        "status", r.getStatus(), "spec", r.getSpec()))
                .toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条产品");
    }

    private AiToolResult queryPartner(Map<String, Object> args) {
        LambdaQueryWrapper<PartnerEntity> q = new LambdaQueryWrapper<PartnerEntity>()
                .eq(PartnerEntity::getTenantId, RequestContext.tenantId())
                .like(text(args, "name") != null, PartnerEntity::getPartnerName, text(args, "name"))
                .eq(text(args, "type") != null, PartnerEntity::getPartnerType, text(args, "type"))
                .eq(readBoolean(args.get("enabled")) != null, PartnerEntity::getStatus, Boolean.TRUE.equals(readBoolean(args.get("enabled"))) ? "ACTIVE" : "INACTIVE")
                .orderByDesc(PartnerEntity::getCreatedAt);
        List<Map<String, Object>> rows = selectLimited(partnerMapper, q, limit(args)).stream()
                .map(r -> map("sourceType", "partner", "sourceId", r.getId(), "bizNo", r.getPartnerCode(), "name", r.getPartnerName(),
                        "type", r.getPartnerType(), "status", r.getStatus(), "contactName", r.getContactName()))
                .toList();
        return rowsResult(rows, "查询到 " + rows.size() + " 条往来单位");
    }

    private AiToolResult getInventoryStats(Map<String, Object> args) {
        LambdaQueryWrapper<InventoryEntity> q = new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                .eq(readLong(args.get("warehouseId")) != null, InventoryEntity::getWarehouseId, readLong(args.get("warehouseId")));
        List<InventoryEntity> rows = inventoryMapper.selectList(q);
        long lowStock = rows.stream().filter(r -> r.getAvailableQty() != null && r.getAvailableQty().compareTo(BigDecimal.TEN) <= 0).count();
        BigDecimal available = rows.stream().map(InventoryEntity::getAvailableQty).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> row = map("sourceType", "inventory_stats", "sourceId", "inventory", "inventoryRows", rows.size(),
                "lowStockCount", lowStock, "availableQtyTotal", available);
        return rowsResult(List.of(row), "库存条目 " + rows.size() + " 条，低库存 " + lowStock + " 条，可用总量 " + available);
    }

    private AiToolResult getDailyReport(Map<String, Object> args, List<String> permissions) {
        LocalDate date = readDate(args.get("date"));
        if (date == null) date = LocalDate.now();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sourceType", "daily_report");
        row.put("sourceId", date.toString());
        if (permissions.contains("INBOUND_READ")) row.put("todayInbound", inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>().eq(InboundOrderEntity::getTenantId, RequestContext.tenantId()).ge(InboundOrderEntity::getCreatedAt, start).lt(InboundOrderEntity::getCreatedAt, end)));
        if (permissions.contains("OUTBOUND_READ")) row.put("todayOutbound", outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>().eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId()).ge(OutboundOrderEntity::getCreatedAt, start).lt(OutboundOrderEntity::getCreatedAt, end)));
        if (permissions.contains("PURCHASE_READ")) row.put("todayPurchase", purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrderEntity>().eq(PurchaseOrderEntity::getTenantId, RequestContext.tenantId()).ge(PurchaseOrderEntity::getCreatedAt, start).lt(PurchaseOrderEntity::getCreatedAt, end)));
        if (permissions.contains("SALES_READ")) row.put("todaySale", salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrderEntity>().eq(SalesOrderEntity::getTenantId, RequestContext.tenantId()).ge(SalesOrderEntity::getCreatedAt, start).lt(SalesOrderEntity::getCreatedAt, end)));
        if (permissions.contains("CS_SESSION_READ")) row.put("todayTickets", ticketMapper.selectCount(new LambdaQueryWrapper<CustomerServiceTicketEntity>().eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId()).ge(CustomerServiceTicketEntity::getCreatedAt, start).lt(CustomerServiceTicketEntity::getCreatedAt, end)));
        return rowsResult(row.size() <= 2 ? List.of() : List.of(row), row.size() <= 2 ? "未查询到相关数据" : "已生成 " + date + " 运营概览");
    }

    private AiToolResult rowsResult(List<Map<String, Object>> rows, String summary) {
        AiToolResult result = new AiToolResult();
        result.setRows(rows);
        result.setSources(rows.stream().limit(5).map(this::sourceFromRow).toList());
        result.setSummary(rows.isEmpty() ? "未查询到相关数据" : summary);
        result.setMessage(result.getSummary());
        result.setEmpty(rows.isEmpty());
        return result;
    }

    private Map<String, Object> sourceFromRow(Map<String, Object> row) {
        return map("sourceType", row.get("sourceType"), "sourceId", row.get("sourceId"), "bizNo", row.getOrDefault("bizNo", row.get("name")),
                "status", row.get("status"), "quantity", row.getOrDefault("availableQty", row.get("availableQtyTotal")));
    }

    private List<Long> productIds(Object productName, Object sku) {
        LambdaQueryWrapper<ProductEntity> q = new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .like(textValue(productName) != null, ProductEntity::getProductName, textValue(productName))
                .like(textValue(sku) != null, ProductEntity::getProductCode, textValue(sku));
        if (textValue(productName) == null && textValue(sku) == null) return List.of();
        return selectLimited(productMapper, q, 20).stream().map(ProductEntity::getId).toList();
    }

    private List<Long> partnerIds(Object name) {
        String text = textValue(name);
        if (text == null) return List.of();
        return selectLimited(partnerMapper, new LambdaQueryWrapper<PartnerEntity>()
                .eq(PartnerEntity::getTenantId, RequestContext.tenantId())
                .like(PartnerEntity::getPartnerName, text), 20).stream().map(PartnerEntity::getId).toList();
    }

    private Map<String, Object> parseArgs(String json) {
        try {
            if (!StringUtils.hasText(json)) return new LinkedHashMap<>();
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private int limit(Map<String, Object> args) {
        Object raw = args.get("limit");
        int value = raw instanceof Number n ? n.intValue() : 10;
        return Math.max(1, Math.min(value, 20));
    }

    private <T> List<T> selectLimited(BaseMapper<T> mapper, LambdaQueryWrapper<T> query, int limit) {
        return mapper.selectPage(new Page<>(1, limit, false), query).getRecords();
    }

    private String text(Map<String, Object> args, String key) { return textValue(args.get(key)); }

    private String textValue(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Long readLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        try { return value == null ? null : Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return null; }
    }

    private Boolean readBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return null;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private LocalDate readDate(Object value) {
        try { return value == null ? null : LocalDate.parse(String.valueOf(value)); } catch (Exception ignored) { return null; }
    }

    private LocalDateTime dateStart(Map<String, Object> args, String key) {
        LocalDate date = readDate(args.get(key));
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime dateEnd(Map<String, Object> args, String key) {
        LocalDate date = readDate(args.get(key));
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    private void writeLogs(AiToolResult result, Long conversationId, Long messageId, String requestId) {
        try {
            AiToolCallLogEntity log = new AiToolCallLogEntity();
            log.setTenantId(RequestContext.tenantId());
            log.setConversationId(conversationId);
            log.setMessageId(messageId);
            log.setRequestId(requestId);
            log.setToolName(result.getToolName());
            log.setArgumentsSummary(result.getArgumentsSummary());
            log.setSuccess(result.isSuccess() ? 1 : 0);
            log.setPermissionResult(result.getPermissionResult());
            log.setDurationMs(result.getDurationMs());
            log.setResultCount(result.getRows().size());
            log.setErrorCode(result.getErrorCode());
            log.setErrorMessage(truncate(result.getErrorMessage(), 512));
            log.setCreatedBy(RequestContext.userId());
            toolLogMapper.insert(log);
            AuditLogEntity audit = new AuditLogEntity();
            audit.setTenantId(RequestContext.tenantId());
            audit.setModule("AI");
            audit.setAction("AI_TOOL_CALL");
            audit.setResourceType("AI_TOOL");
            audit.setResourceId(result.getToolName());
            audit.setBizNo(requestId);
            audit.setOperatorId(RequestContext.userId());
            audit.setAfterJson(objectMapper.writeValueAsString(result.toMap()));
            audit.setOccurredAt(LocalDateTime.now());
            audit.setCreatedBy(RequestContext.userId());
            audit.setUpdatedBy(RequestContext.userId());
            auditLogMapper.insert(audit);
        } catch (Exception ignored) {
            // Tool logging must never break chat.
        }
    }

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            if (kv[i + 1] != null) m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    private String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max);
    }
}
