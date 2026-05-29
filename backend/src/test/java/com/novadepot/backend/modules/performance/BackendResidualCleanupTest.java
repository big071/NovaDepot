package com.novadepot.backend.modules.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AgentTaskRunEntity;
import com.novadepot.backend.model.entity.NotificationEntity;
import com.novadepot.backend.modules.agent.AgentCenterService;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.knowledge.KnowledgeService;
import com.novadepot.backend.modules.notifications.NotificationsService;
import com.novadepot.backend.modules.reports.AmountSummaryMetric;
import com.novadepot.backend.modules.reports.ReportsService;
import com.novadepot.backend.repository.AgentTaskRunMapper;
import com.novadepot.backend.repository.AuditLogMapper;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.CustomerServiceMessageMapper;
import com.novadepot.backend.repository.CustomerServiceSessionMapper;
import com.novadepot.backend.repository.CustomerServiceTicketMapper;
import com.novadepot.backend.repository.FAQKnowledgeMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.NotificationMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.ProductMapper;
import com.novadepot.backend.repository.PurchaseOrderMapper;
import com.novadepot.backend.repository.SalesOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendResidualCleanupTest {

    @BeforeEach
    void setUp() {
        RequestContext.setTenantId(1L);
        RequestContext.setUserId(100L);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void agentRunList_usesParameterizedMapperAndKeepsPageShape() {
        AgentTaskRunMapper runMapper = mock(AgentTaskRunMapper.class);
        AgentCenterService service = new AgentCenterService(
                runMapper,
                mock(InventoryMapper.class),
                mock(ProductMapper.class),
                mock(InboundOrderMapper.class),
                mock(OutboundOrderMapper.class),
                mock(CustomerServiceTicketMapper.class),
                mock(FAQKnowledgeMapper.class),
                mock(AuditLogRecordService.class),
                mock(KnowledgeService.class)
        );
        AgentTaskRunEntity row = new AgentTaskRunEntity();
        row.setId(10L);
        row.setTaskCode("LOW_STOCK_ANALYSIS");
        row.setTaskName("低库存分析");
        row.setStatus("SUCCESS");
        row.setStartedAt(LocalDateTime.of(2026, 5, 29, 10, 0));
        when(runMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(runMapper.selectRunsPage(eq(1L), eq("LOW_STOCK_ANALYSIS"), eq("SUCCESS"), eq(0), eq(20)))
                .thenReturn(List.of(row));

        Map<String, Object> result = service.listRuns(1, 20, "low_stock_analysis", "success");

        assertThat(result).containsEntry("total", 1L);
        assertThat(result).containsEntry("pageNo", 1);
        assertThat(result).containsEntry("pageSize", 20);
        assertThat((List<?>) result.get("list")).hasSize(1);
        verify(runMapper).selectRunsPage(1L, "LOW_STOCK_ANALYSIS", "SUCCESS", 0, 20);
    }

    @Test
    void notificationList_usesParameterizedMapperAndKeepsPageShape() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        NotificationsService service = new NotificationsService(mapper, mock(AuditLogRecordService.class));
        NotificationEntity row = new NotificationEntity();
        row.setId(20L);
        row.setNotifyType("LOW_STOCK");
        row.setTitle("低库存提醒");
        row.setContent("请处理");
        row.setReadFlag(0);
        row.setSentAt(LocalDateTime.of(2026, 5, 29, 11, 0));
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(mapper.selectMinePage(eq(1L), eq(100L), eq(true), eq(20), eq(20))).thenReturn(List.of(row));

        Map<String, Object> result = service.list(true, 2, 20);

        assertThat(result).containsEntry("total", 1L);
        assertThat(result).containsEntry("pageNo", 2);
        assertThat(result).containsEntry("pageSize", 20);
        assertThat((List<?>) result.get("list")).hasSize(1);
        verify(mapper).selectMinePage(1L, 100L, true, 20, 20);
    }

    @Test
    void purchaseSalesSummary_usesSqlSummariesAndKeepsRowsShape() {
        PurchaseOrderMapper purchaseMapper = mock(PurchaseOrderMapper.class);
        SalesOrderMapper salesMapper = mock(SalesOrderMapper.class);
        ReportsService service = reportsService(purchaseMapper, salesMapper, mock(InboundOrderMapper.class), mock(OutboundOrderMapper.class), mock(CustomerServiceTicketMapper.class));
        AmountSummaryMetric purchase = new AmountSummaryMetric();
        purchase.setCount(2L);
        purchase.setAmount(new BigDecimal("120.50"));
        AmountSummaryMetric sales = new AmountSummaryMetric();
        sales.setCount(3L);
        sales.setAmount(new BigDecimal("200.00"));
        when(purchaseMapper.selectAmountSummary(eq(1L), eq(9L), any(), any())).thenReturn(purchase);
        when(salesMapper.selectAmountSummary(eq(1L), eq(9L), any(), any())).thenReturn(sales);

        Map<String, Object> result = service.purchaseSalesSummary("2026-05-01", "2026-05-29", 9L);

        assertThat(result).containsEntry("reportName", "purchaseSalesSummary");
        assertThat(result).containsEntry("total", 3);
        List<?> rows = (List<?>) result.get("rows");
        assertThat(rows).hasSize(3);
        Map<?, ?> netRow = (Map<?, ?>) rows.get(2);
        assertThat(netRow.get("count")).isEqualTo(1L);
        assertThat(netRow.get("amount")).isEqualTo(new BigDecimal("79.50"));
        verify(purchaseMapper).selectAmountSummary(eq(1L), eq(9L), any(), any());
        verify(salesMapper).selectAmountSummary(eq(1L), eq(9L), any(), any());
    }

    private ReportsService reportsService(PurchaseOrderMapper purchaseMapper,
                                          SalesOrderMapper salesMapper,
                                          InboundOrderMapper inboundMapper,
                                          OutboundOrderMapper outboundMapper,
                                          CustomerServiceTicketMapper ticketMapper) {
        return new ReportsService(
                mock(ProductMapper.class),
                mock(InventoryMapper.class),
                inboundMapper,
                outboundMapper,
                mock(InventoryTransactionMapper.class),
                purchaseMapper,
                salesMapper,
                mock(com.novadepot.backend.modules.inventory.LowStockPolicyService.class),
                mock(AuthQueryMapper.class),
                mock(AuditLogMapper.class),
                mock(CustomerServiceSessionMapper.class),
                ticketMapper,
                mock(CustomerServiceMessageMapper.class),
                mock(AuditLogRecordService.class)
        );
    }
}
