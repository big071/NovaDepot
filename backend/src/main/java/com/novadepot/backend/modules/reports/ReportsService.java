package com.novadepot.backend.modules.reports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AuditLogEntity;
import com.novadepot.backend.model.entity.CustomerServiceMessageEntity;
import com.novadepot.backend.model.entity.CustomerServiceSessionEntity;
import com.novadepot.backend.model.entity.CustomerServiceTicketEntity;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.model.entity.PurchaseOrderEntity;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.inventory.LowStockPolicyService;
import com.novadepot.backend.repository.AuditLogMapper;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.CustomerServiceMessageMapper;
import com.novadepot.backend.repository.CustomerServiceSessionMapper;
import com.novadepot.backend.repository.CustomerServiceTicketMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.ProductMapper;
import com.novadepot.backend.repository.PurchaseOrderMapper;
import com.novadepot.backend.repository.SalesOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportsService {
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final LowStockPolicyService lowStockPolicyService;
    private final AuthQueryMapper authQueryMapper;
    private final AuditLogMapper auditLogMapper;
    private final CustomerServiceSessionMapper customerServiceSessionMapper;
    private final CustomerServiceTicketMapper customerServiceTicketMapper;
    private final CustomerServiceMessageMapper customerServiceMessageMapper;
    private final AuditLogRecordService auditLogRecordService;

    public ReportsService(ProductMapper productMapper,
                          InventoryMapper inventoryMapper,
                          InboundOrderMapper inboundOrderMapper,
                          OutboundOrderMapper outboundOrderMapper,
                          InventoryTransactionMapper inventoryTransactionMapper,
                          PurchaseOrderMapper purchaseOrderMapper,
                          SalesOrderMapper salesOrderMapper,
                          LowStockPolicyService lowStockPolicyService,
                          AuthQueryMapper authQueryMapper,
                          AuditLogMapper auditLogMapper,
                          CustomerServiceSessionMapper customerServiceSessionMapper,
                          CustomerServiceTicketMapper customerServiceTicketMapper,
                          CustomerServiceMessageMapper customerServiceMessageMapper,
                          AuditLogRecordService auditLogRecordService) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.lowStockPolicyService = lowStockPolicyService;
        this.authQueryMapper = authQueryMapper;
        this.auditLogMapper = auditLogMapper;
        this.customerServiceSessionMapper = customerServiceSessionMapper;
        this.customerServiceTicketMapper = customerServiceTicketMapper;
        this.customerServiceMessageMapper = customerServiceMessageMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public Map<String, Object> dashboard() {
        Long tenantId = RequestContext.tenantId();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long totalSku = productMapper.selectCount(new LambdaQueryWrapper<com.novadepot.backend.model.entity.ProductEntity>()
                .eq(com.novadepot.backend.model.entity.ProductEntity::getTenantId, tenantId));

        long todayInbound = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, tenantId)
                .ge(InboundOrderEntity::getCreatedAt, start)
                .lt(InboundOrderEntity::getCreatedAt, end));

        long todayOutbound = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, tenantId)
                .ge(OutboundOrderEntity::getCreatedAt, start)
                .lt(OutboundOrderEntity::getCreatedAt, end));

        List<InventoryEntity> inventoryRows = inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, tenantId));
        long lowStockCount = lowStockPolicyService.countLowStock(
                inventoryRows,
                lowStockPolicyService.buildProductMapFromInventory(inventoryRows)
        );

        return Map.of(
                "totalSku", totalSku,
                "todayInbound", todayInbound,
                "todayOutbound", todayOutbound,
                "lowStockCount", lowStockCount
        );
    }

    public Map<String, Object> workbenchTodos() {
        Long tenantId = RequestContext.tenantId();
        Long userId = RequestContext.userId();
        String roleKey = resolveRoleKey(authQueryMapper.findRoleCodes(tenantId, userId));
        Map<String, Object> metrics = dashboard();

        long pendingInboundApproval = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, tenantId)
                .eq(InboundOrderEntity::getStatus, "SUBMITTED"));
        long pendingOutboundApproval = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, tenantId)
                .eq(OutboundOrderEntity::getStatus, "SUBMITTED"));
        long rejectedDocs = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, tenantId)
                .eq(InboundOrderEntity::getStatus, "REJECTED"))
                + outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, tenantId)
                .eq(OutboundOrderEntity::getStatus, "REJECTED"));
        long draftDocs = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, tenantId)
                .eq(InboundOrderEntity::getStatus, "DRAFT"))
                + outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, tenantId)
                .eq(OutboundOrderEntity::getStatus, "DRAFT"));
        long pendingExecution = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, tenantId)
                .eq(InboundOrderEntity::getStatus, "APPROVED"))
                + outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, tenantId)
                .eq(OutboundOrderEntity::getStatus, "APPROVED"));
        long recentAuditFailures = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getTenantId, tenantId)
                .like(AuditLogEntity::getAction, "FAIL")
                .ge(AuditLogEntity::getOccurredAt, LocalDateTime.now().minusDays(3)));
        long pendingSessions = customerServiceSessionMapper.selectCount(new LambdaQueryWrapper<CustomerServiceSessionEntity>()
                .eq(CustomerServiceSessionEntity::getTenantId, tenantId)
                .in(CustomerServiceSessionEntity::getStatus, List.of("OPEN", "IN_PROGRESS", "PROCESSING")));
        long pendingTickets = customerServiceTicketMapper.selectCount(new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                .eq(CustomerServiceTicketEntity::getTenantId, tenantId)
                .in(CustomerServiceTicketEntity::getStatus, List.of("OPEN", "PROCESSING")));
        long aiPendingConfirm = customerServiceMessageMapper.selectCount(new LambdaQueryWrapper<CustomerServiceMessageEntity>()
                .eq(CustomerServiceMessageEntity::getTenantId, tenantId)
                .eq(CustomerServiceMessageEntity::getMsgType, "AI_AUTO_REPLY")
                .ge(CustomerServiceMessageEntity::getCreatedAt, LocalDateTime.now().minusDays(1)));

        Map<String, Object> todos = new LinkedHashMap<>();
        if ("admin".equals(roleKey)) {
            todos.put("pendingInboundApproval", pendingInboundApproval);
            todos.put("pendingOutboundApproval", pendingOutboundApproval);
            todos.put("pendingExceptions", rejectedDocs);
            todos.put("recentAuditFailures", recentAuditFailures);
        } else if ("warehouse_ops".equals(roleKey)) {
            todos.put("pendingDrafts", draftDocs);
            todos.put("rejectedDocuments", rejectedDocs);
            todos.put("pendingExecution", pendingExecution);
            todos.put("lowStockCount", metrics.get("lowStockCount"));
        } else if ("cs_ops".equals(roleKey)) {
            todos.put("pendingReplies", pendingSessions);
            todos.put("pendingTickets", pendingTickets);
            todos.put("aiPendingConfirm", aiPendingConfirm);
        } else {
            todos.put("riskSummary", Map.of(
                    "lowStockCount", metrics.get("lowStockCount"),
                    "pendingApproval", pendingInboundApproval + pendingOutboundApproval,
                    "auditFailures", recentAuditFailures
            ));
            todos.put("todayOverview", Map.of(
                    "todayInbound", metrics.get("todayInbound"),
                    "todayOutbound", metrics.get("todayOutbound"),
                    "totalSku", metrics.get("totalSku")
            ));
        }
        return Map.of("roleKey", roleKey, "todos", todos, "metrics", metrics);
    }

    private String resolveRoleKey(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) return "observer";
        Set<String> normalized = roleCodes.stream().map(v -> v == null ? "" : v.trim().toUpperCase()).collect(java.util.stream.Collectors.toSet());
        if (normalized.contains("TENANT_ADMIN")) return "admin";
        if (normalized.contains("WAREHOUSE_MANAGER") || normalized.contains("WAREHOUSE_OPERATOR")) return "warehouse_ops";
        if (normalized.contains("CS_AGENT")) return "cs_ops";
        return "observer";
    }

    public Map<String, Object> inventoryTurnover(String dateFrom, String dateTo, Long warehouseId) {
        DateRange range = parseRange(dateFrom, dateTo);
        Map<Long, BigDecimal> outboundQty = inventoryTransactionMapper
                .sumOutboundByProduct(RequestContext.tenantId(), warehouseId, range.start(), range.end())
                .stream()
                .collect(Collectors.toMap(InventoryTurnoverMetric::getProductId, row -> value(row.getOutboundQty()), (a, b) -> a, LinkedHashMap::new));
        Map<Long, BigDecimal> stockQty = inventoryMapper
                .sumAvailableByProduct(RequestContext.tenantId(), warehouseId)
                .stream()
                .collect(Collectors.toMap(InventoryTurnoverMetric::getProductId, row -> value(row.getAvailableQty()), (a, b) -> a, LinkedHashMap::new));
        Set<Long> productIds = new java.util.HashSet<>();
        productIds.addAll(outboundQty.keySet());
        productIds.addAll(stockQty.keySet());
        Map<Long, String> productNames = productIds.isEmpty()
                ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .filter(com.novadepot.backend.model.entity.ProductEntity.class::isInstance)
                .map(com.novadepot.backend.model.entity.ProductEntity.class::cast)
                .collect(Collectors.toMap(com.novadepot.backend.model.entity.ProductEntity::getId, p -> p.getProductCode() + " / " + p.getProductName(), (a, b) -> a));
        List<Map<String, Object>> rows = productIds.stream().map(id -> {
            BigDecimal outbound = outboundQty.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal stock = stockQty.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal turnover = stock.signum() == 0 ? BigDecimal.ZERO : outbound.divide(stock, 4, java.math.RoundingMode.HALF_UP);
            return row("productId", id, "productName", productNames.getOrDefault(id, String.valueOf(id)), "outboundQty", outbound, "availableQty", stock, "turnoverRate", turnover);
        }).sorted(Comparator.comparing(item -> String.valueOf(item.get("productName")))).toList();
        return report("inventoryTurnover", range, rows);
    }

    public Map<String, Object> inoutSummary(String dateFrom, String dateTo, String grain) {
        DateRange range = parseRange(dateFrom, dateTo);
        boolean week = "WEEK".equalsIgnoreCase(grain);
        Map<String, Long> inboundMap = groupPeriodCounts(inboundOrderMapper.countByDay(RequestContext.tenantId(), range.start(), range.end()), week);
        Map<String, Long> outboundMap = groupPeriodCounts(outboundOrderMapper.countByDay(RequestContext.tenantId(), range.start(), range.end()), week);
        Set<String> keys = new java.util.TreeSet<>();
        keys.addAll(inboundMap.keySet());
        keys.addAll(outboundMap.keySet());
        List<Map<String, Object>> rows = keys.stream().map(k -> row("period", k, "inboundCount", inboundMap.getOrDefault(k, 0L), "outboundCount", outboundMap.getOrDefault(k, 0L), "netCount", inboundMap.getOrDefault(k, 0L) - outboundMap.getOrDefault(k, 0L))).toList();
        return report("inoutSummary", range, rows);
    }

    public Map<String, Object> purchaseSalesSummary(String dateFrom, String dateTo, Long partnerId) {
        DateRange range = parseRange(dateFrom, dateTo);
        AmountSummaryMetric purchase = purchaseOrderMapper.selectAmountSummary(RequestContext.tenantId(), partnerId, range.start(), range.end());
        AmountSummaryMetric sale = salesOrderMapper.selectAmountSummary(RequestContext.tenantId(), partnerId, range.start(), range.end());
        long purchaseCount = countValue(purchase == null ? null : purchase.getCount());
        long salesCount = countValue(sale == null ? null : sale.getCount());
        BigDecimal purchaseAmount = value(purchase == null ? null : purchase.getAmount());
        BigDecimal salesAmount = value(sale == null ? null : sale.getAmount());
        List<Map<String, Object>> rows = List.of(
                row("type", "采购", "count", purchaseCount, "amount", purchaseAmount),
                row("type", "销售", "count", salesCount, "amount", salesAmount),
                row("type", "净额", "count", salesCount - purchaseCount, "amount", salesAmount.subtract(purchaseAmount))
        );
        return report("purchaseSalesSummary", range, rows);
    }

    public Map<String, Object> ticketEfficiency(String dateFrom, String dateTo, Long assigneeId) {
        DateRange range = parseRange(dateFrom, dateTo);
        List<Map<String, Object>> rows = customerServiceTicketMapper
                .selectEfficiencyByAssignee(RequestContext.tenantId(), assigneeId, range.start(), range.end())
                .stream()
                .map(entry -> {
            long total = countValue(entry.getTicketCount());
            long closed = countValue(entry.getClosedCount());
            return row("assigneeId", entry.getAssigneeId(), "ticketCount", total, "closedCount", closed, "closeRate", total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(closed).divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP));
        }).toList();
        return report("ticketEfficiency", range, rows);
    }

    public String inventoryTurnoverCsv(String dateFrom, String dateTo, Long warehouseId) {
        auditExport("INVENTORY_TURNOVER");
        return csv((List<Map<String, Object>>) inventoryTurnover(dateFrom, dateTo, warehouseId).get("rows"));
    }

    public String inoutSummaryCsv(String dateFrom, String dateTo, String grain) {
        auditExport("INOUT_SUMMARY");
        return csv((List<Map<String, Object>>) inoutSummary(dateFrom, dateTo, grain).get("rows"));
    }

    public String purchaseSalesSummaryCsv(String dateFrom, String dateTo, Long partnerId) {
        auditExport("PURCHASE_SALES_SUMMARY");
        return csv((List<Map<String, Object>>) purchaseSalesSummary(dateFrom, dateTo, partnerId).get("rows"));
    }

    public String ticketEfficiencyCsv(String dateFrom, String dateTo, Long assigneeId) {
        auditExport("TICKET_EFFICIENCY");
        return csv((List<Map<String, Object>>) ticketEfficiency(dateFrom, dateTo, assigneeId).get("rows"));
    }

    private Map<String, Object> report(String name, DateRange range, List<Map<String, Object>> rows) {
        return Map.of("reportName", name, "dateFrom", range.start().toLocalDate().toString(), "dateTo", range.end().minusDays(1).toLocalDate().toString(), "rows", rows, "total", rows.size());
    }

    private DateRange parseRange(String dateFrom, String dateTo) {
        LocalDate to = StringUtils.hasText(dateTo) ? LocalDate.parse(dateTo) : LocalDate.now();
        LocalDate from = StringUtils.hasText(dateFrom) ? LocalDate.parse(dateFrom) : to.minusDays(6);
        return new DateRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private String bucket(LocalDateTime time, boolean week) {
        if (time == null) return "-";
        LocalDate date = time.toLocalDate();
        return bucket(date, week);
    }

    private String bucket(LocalDate date, boolean week) {
        if (date == null) return "-";
        if (!week) return date.toString();
        int weekNo = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        return date.getYear() + "-W" + String.format("%02d", weekNo);
    }

    private Map<String, Long> groupPeriodCounts(List<PeriodCountMetric> counts, boolean week) {
        if (counts == null || counts.isEmpty()) {
            return Map.of();
        }
        return counts.stream().collect(Collectors.groupingBy(
                item -> bucket(LocalDate.parse(item.getPeriod()), week),
                LinkedHashMap::new,
                Collectors.summingLong(item -> countValue(item.getCount()))
        ));
    }

    private long countValue(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal abs(BigDecimal value) {
        return value(value).abs();
    }

    private Map<String, Object> row(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private String csv(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "\uFEFF暂无数据\n";
        }
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder(Math.max(64, rows.size() * headers.size() * 16));
        sb.append("\uFEFF");
        sb.append(String.join(",", headers)).append("\n");
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(escape(row.get(headers.get(i))));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String escape(Object raw) {
        String text = raw == null ? "" : String.valueOf(raw);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void auditExport(String reportCode) {
        auditLogRecordService.record("REPORT", "EXPORT", "REPORT", reportCode, reportCode, null, "{\"report\":\"" + reportCode + "\"}");
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {}
}
