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
import com.novadepot.backend.modules.inventory.LowStockPolicyService;
import com.novadepot.backend.repository.AuditLogMapper;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.CustomerServiceMessageMapper;
import com.novadepot.backend.repository.CustomerServiceSessionMapper;
import com.novadepot.backend.repository.CustomerServiceTicketMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.ProductMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReportsService {
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final LowStockPolicyService lowStockPolicyService;
    private final AuthQueryMapper authQueryMapper;
    private final AuditLogMapper auditLogMapper;
    private final CustomerServiceSessionMapper customerServiceSessionMapper;
    private final CustomerServiceTicketMapper customerServiceTicketMapper;
    private final CustomerServiceMessageMapper customerServiceMessageMapper;

    public ReportsService(ProductMapper productMapper,
                          InventoryMapper inventoryMapper,
                          InboundOrderMapper inboundOrderMapper,
                          OutboundOrderMapper outboundOrderMapper,
                          LowStockPolicyService lowStockPolicyService,
                          AuthQueryMapper authQueryMapper,
                          AuditLogMapper auditLogMapper,
                          CustomerServiceSessionMapper customerServiceSessionMapper,
                          CustomerServiceTicketMapper customerServiceTicketMapper,
                          CustomerServiceMessageMapper customerServiceMessageMapper) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.lowStockPolicyService = lowStockPolicyService;
        this.authQueryMapper = authQueryMapper;
        this.auditLogMapper = auditLogMapper;
        this.customerServiceSessionMapper = customerServiceSessionMapper;
        this.customerServiceTicketMapper = customerServiceTicketMapper;
        this.customerServiceMessageMapper = customerServiceMessageMapper;
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
}
