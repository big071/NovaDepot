package com.novadepot.backend.modules.agent.patrol;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.CustomerServiceTicketEntity;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.model.entity.PurchaseOrderEntity;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.inventory.LowStockPolicyService;
import com.novadepot.backend.modules.notifications.NotificationsService;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.CustomerServiceTicketMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.ProductMapper;
import com.novadepot.backend.repository.PurchaseOrderMapper;
import com.novadepot.backend.repository.SalesOrderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentPatrolService {
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final CustomerServiceTicketMapper customerServiceTicketMapper;
    private final LowStockPolicyService lowStockPolicyService;
    private final NotificationsService notificationsService;
    private final AuthQueryMapper authQueryMapper;
    private final AuditLogRecordService auditLogRecordService;

    public AgentPatrolService(InventoryMapper inventoryMapper,
                              ProductMapper productMapper,
                              InboundOrderMapper inboundOrderMapper,
                              OutboundOrderMapper outboundOrderMapper,
                              PurchaseOrderMapper purchaseOrderMapper,
                              SalesOrderMapper salesOrderMapper,
                              CustomerServiceTicketMapper customerServiceTicketMapper,
                              LowStockPolicyService lowStockPolicyService,
                              NotificationsService notificationsService,
                              AuthQueryMapper authQueryMapper,
                              AuditLogRecordService auditLogRecordService) {
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.customerServiceTicketMapper = customerServiceTicketMapper;
        this.lowStockPolicyService = lowStockPolicyService;
        this.notificationsService = notificationsService;
        this.authQueryMapper = authQueryMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public Map<String, Object> runLowStockPatrol() {
        withSystemContext();
        auditLogRecordService.record("AGENT_PATROL", "LOW_STOCK_START", "AGENT_PATROL", "LOW_STOCK", null, null, "{}");
        List<InventoryEntity> rows = inventoryMapper.selectLowestAvailable(RequestContext.tenantId(), 500);
        Map<Long, ProductEntity> productMap = lowStockPolicyService.buildProductMapFromInventory(rows);
        List<InventoryEntity> lowRows = rows.stream()
                .filter(row -> lowStockPolicyService.isLowStock(row, productMap))
                .limit(20)
                .toList();
        int created = notifyUsers("LOW_STOCK_PATROL", "LOW_STOCK", "LOW-STOCK",
                "低库存巡检提醒",
                "Agent 巡检发现 " + lowRows.size() + " 条低库存记录，请查看库存预警。",
                "WARNING",
                "/wms/inventory?from=notification&focus=low-stock");
        auditLogRecordService.record("AGENT_PATROL", "LOW_STOCK_SUCCESS", "AGENT_PATROL", "LOW_STOCK", null, null, "{\"lowStockCount\":" + lowRows.size() + ",\"notifications\":" + created + "}");
        return Map.of("patrolType", "LOW_STOCK_PATROL", "lowStockCount", lowRows.size(), "notifications", created);
    }

    public Map<String, Object> runOrderOverduePatrol(int overdueHours) {
        withSystemContext();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(Math.max(1, overdueHours));
        auditLogRecordService.record("AGENT_PATROL", "ORDER_OVERDUE_START", "AGENT_PATROL", "ORDER_OVERDUE", null, null, "{\"overdueHours\":" + overdueHours + "}");
        int count = 0;
        count += notifyInbound(cutoff);
        count += notifyOutbound(cutoff);
        count += notifyPurchase(cutoff);
        count += notifySales(cutoff);
        auditLogRecordService.record("AGENT_PATROL", "ORDER_OVERDUE_SUCCESS", "AGENT_PATROL", "ORDER_OVERDUE", null, null, "{\"notifications\":" + count + "}");
        return Map.of("patrolType", "ORDER_OVERDUE_PATROL", "notifications", count, "overdueHours", overdueHours);
    }

    public Map<String, Object> runTicketOverduePatrol(int overdueHours) {
        withSystemContext();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(Math.max(1, overdueHours));
        List<CustomerServiceTicketEntity> tickets = customerServiceTicketMapper.selectOverdueForPatrol(RequestContext.tenantId(), cutoff, 50);
        int created = 0;
        for (CustomerServiceTicketEntity ticket : tickets) {
            created += notifyUsers("TICKET_OVERDUE_PATROL", "CS_TICKET", ticket.getTicketNo(),
                    "工单超时提醒",
                    "工单 " + ticket.getTicketNo() + " 已超过 " + overdueHours + " 小时未完成，请跟进处理。",
                    "WARNING",
                    "/cs/workspace?ticketNo=" + ticket.getTicketNo());
        }
        auditLogRecordService.record("AGENT_PATROL", "TICKET_OVERDUE_SUCCESS", "AGENT_PATROL", "TICKET_OVERDUE", null, null, "{\"tickets\":" + tickets.size() + ",\"notifications\":" + created + "}");
        return Map.of("patrolType", "TICKET_OVERDUE_PATROL", "tickets", tickets.size(), "notifications", created);
    }

    private int notifyInbound(LocalDateTime cutoff) {
        return inboundOrderMapper.selectOverdueForPatrol(RequestContext.tenantId(), cutoff, 50)
                .stream()
                .mapToInt(row -> notifyUsers("ORDER_OVERDUE_PATROL", "INBOUND_ORDER", row.getInboundNo(), "入库单超时提醒", "入库单 " + row.getInboundNo() + " 待处理已超时。", "WARNING", "/wms/inbound"))
                .sum();
    }

    private int notifyOutbound(LocalDateTime cutoff) {
        return outboundOrderMapper.selectOverdueForPatrol(RequestContext.tenantId(), cutoff, 50)
                .stream()
                .mapToInt(row -> notifyUsers("ORDER_OVERDUE_PATROL", "OUTBOUND_ORDER", row.getOutboundNo(), "出库单超时提醒", "出库单 " + row.getOutboundNo() + " 待处理已超时。", "WARNING", "/wms/outbound"))
                .sum();
    }

    private int notifyPurchase(LocalDateTime cutoff) {
        return purchaseOrderMapper.selectOverdueForPatrol(RequestContext.tenantId(), cutoff, 50)
                .stream()
                .mapToInt(row -> notifyUsers("ORDER_OVERDUE_PATROL", "PURCHASE_ORDER", row.getPurchaseNo(), "采购单超时提醒", "采购单 " + row.getPurchaseNo() + " 待处理已超时。", "WARNING", "/erp/purchases"))
                .sum();
    }

    private int notifySales(LocalDateTime cutoff) {
        return salesOrderMapper.selectOverdueForPatrol(RequestContext.tenantId(), cutoff, 50)
                .stream()
                .mapToInt(row -> notifyUsers("ORDER_OVERDUE_PATROL", "SALES_ORDER", row.getSalesNo(), "销售单超时提醒", "销售单 " + row.getSalesNo() + " 待处理已超时。", "WARNING", "/erp/sales"))
                .sum();
    }

    private int notifyUsers(String notifyType, String bizType, String bizNo, String title, String content, String severity, String jumpPath) {
        List<Long> userIds = authQueryMapper.findUserIdsByPermission(RequestContext.tenantId(), "NOTIFY_READ");
        int created = 0;
        for (Long userId : userIds) {
            notificationsService.createIfAbsent(userId, notifyType, bizType, bizNo, title, content, severity, jumpPath);
            created++;
        }
        return created;
    }

    private void withSystemContext() {
        RequestContext.setTenantId(1L);
        RequestContext.setUserId(1L);
    }
}
