package com.novadepot.backend.modules.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.AgentTaskRunEntity;
import com.novadepot.backend.model.entity.CustomerServiceTicketEntity;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.model.entity.FAQKnowledgeEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.knowledge.KnowledgeService;
import com.novadepot.backend.repository.AgentTaskRunMapper;
import com.novadepot.backend.repository.CustomerServiceTicketMapper;
import com.novadepot.backend.repository.FAQKnowledgeMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentCenterService {
    private static final String TASK_REPLENISH = "REPLENISH_SUGGESTION";
    private static final String TASK_LOW_STOCK = "LOW_STOCK_ANALYSIS";
    private static final String TASK_ANOMALY = "ANOMALY_PATROL";
    private static final String TASK_DAILY = "DAILY_OPERATIONS_REPORT";
    private static final String TASK_TICKET_TRIAGE = "CS_TICKET_TRIAGE";
    private static final String TASK_SOP = "SOP_RECOMMENDATION";

    private final AgentTaskRunMapper agentTaskRunMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final CustomerServiceTicketMapper customerServiceTicketMapper;
    private final FAQKnowledgeMapper faqKnowledgeMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentCenterService(AgentTaskRunMapper agentTaskRunMapper,
                              InventoryMapper inventoryMapper,
                              ProductMapper productMapper,
                              InboundOrderMapper inboundOrderMapper,
                              OutboundOrderMapper outboundOrderMapper,
                              CustomerServiceTicketMapper customerServiceTicketMapper,
                              FAQKnowledgeMapper faqKnowledgeMapper,
                              AuditLogRecordService auditLogRecordService,
                              KnowledgeService knowledgeService) {
        this.agentTaskRunMapper = agentTaskRunMapper;
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.customerServiceTicketMapper = customerServiceTicketMapper;
        this.faqKnowledgeMapper = faqKnowledgeMapper;
        this.auditLogRecordService = auditLogRecordService;
        this.knowledgeService = knowledgeService;
    }

    public List<Map<String, Object>> listTasks() {
        return List.of(
                taskInfo(TASK_LOW_STOCK, "低库存分析", "分析低库存清单与风险优先级",
                        "分析低库存记录并给出风险排序。",
                        List.of("inventory", "products", "outbound_orders"),
                        "低库存风险摘要 + 优先处理清单",
                        List.of(
                                taskParam("limit", "建议输出条数", "返回多少条重点风险商品，建议 3-10 条。", "number", 5),
                                taskParam("recentDays", "近几天出库趋势", "读取近 N 天出库趋势辅助判断风险。", "number", 7)
                        )),
                taskInfo(TASK_REPLENISH, "补货建议", "分析低库存并输出补货数量建议",
                        "根据低库存与安全阈值生成补货建议。",
                        List.of("inventory", "products"),
                        "补货建议列表（商品、当前库存、建议补货量、理由）",
                        List.of(
                                taskParam("limit", "建议输出条数", "返回多少条补货建议。", "number", 5),
                                taskParam("lowStockThreshold", "低库存阈值", "未配置安全库存时的兜底阈值。", "number", 10)
                        )),
                taskInfo(TASK_ANOMALY, "异常巡检", "巡检负库存、低库存和异常状态单据",
                        "扫描库存与单据异常并输出清单。",
                        List.of("inventory", "inbound_orders", "outbound_orders"),
                        "异常指标汇总 + 异常明细样本",
                        List.of(
                                taskParam("staleDays", "巡检窗口天数", "检查多少天内未完成的单据。", "number", 3),
                                taskParam("lowStockThreshold", "低库存阈值", "低库存判断阈值。", "number", 10)
                        )),
                taskInfo(TASK_DAILY, "运营日报", "汇总当日入库、出库、工单与低库存",
                        "自动生成指定日期的运营日报。",
                        List.of("inbound_orders", "outbound_orders", "customer_service_tickets", "inventory"),
                        "日报摘要（结论 + 指标分段）",
                        List.of(taskParam("date", "日报日期", "不填默认今天，格式 YYYY-MM-DD。", "date", ""))),
                taskInfo(TASK_TICKET_TRIAGE, "工单处理建议", "按工单内容给出分类、优先级与处理建议",
                        "对待处理工单给出分类、优先级和建议动作。",
                        List.of("customer_service_tickets", "faq_knowledge"),
                        "工单分类建议 + 优先级建议 + 处理动作建议",
                        List.of(taskParam("limit", "建议输出条数", "返回多少条待处理工单建议。", "number", 5))),
                taskInfo(TASK_SOP, "SOP 建议", "根据主题生成标准处理步骤建议",
                        "按主题给出可执行 SOP 步骤。",
                        List.of("faq_knowledge", "customer_service_tickets"),
                        "SOP 步骤、风险点和复核动作",
                        List.of(taskParam("topic", "SOP主题", "例如：物流催发、退换货、库存异常。", "text", "")))
        );
    }

    public Map<String, Object> executeTask(String taskCode, Map<String, Object> target) {
        String normalizedTaskCode = normalizeTaskCode(taskCode);
        String taskName = taskNameByCode(normalizedTaskCode);
        Map<String, Object> safeTarget = target == null ? new LinkedHashMap<>() : new LinkedHashMap<>(target);

        AgentTaskRunEntity run = new AgentTaskRunEntity();
        run.setTenantId(RequestContext.tenantId());
        run.setTaskCode(normalizedTaskCode);
        run.setTaskName(taskName);
        run.setStatus("RUNNING");
        run.setTargetJson(toJson(safeTarget));
        run.setStartedAt(LocalDateTime.now());
        run.setCreatedBy(RequestContext.userId());
        run.setUpdatedBy(RequestContext.userId());
        agentTaskRunMapper.insert(run);

        auditLogRecordService.record(
                "AGENT",
                "TASK_EXECUTE_START",
                "AGENT_TASK_RUN",
                String.valueOf(run.getId()),
                normalizedTaskCode,
                null,
                toJson(Map.of("taskCode", normalizedTaskCode, "target", safeTarget))
        );

        List<Map<String, Object>> steps = new ArrayList<>();
        try {
            Map<String, Object> result = switch (normalizedTaskCode) {
                case TASK_LOW_STOCK -> runLowStockAnalysis(safeTarget, steps);
                case TASK_REPLENISH -> runReplenishSuggestion(safeTarget, steps);
                case TASK_ANOMALY -> runAnomalyPatrol(safeTarget, steps);
                case TASK_DAILY -> runDailyOpsReport(safeTarget, steps);
                case TASK_TICKET_TRIAGE -> runTicketTriage(safeTarget, steps);
                case TASK_SOP -> runSopRecommendation(safeTarget, steps);
                default -> throw new BizException(ErrorCode.BIZ_ERROR.code(), "不支持的任务类型");
            };

            run.setStatus("SUCCESS");
            run.setStepsJson(toJson(steps));
            run.setResultJson(toJson(result));
            run.setFinishedAt(LocalDateTime.now());
            run.setUpdatedBy(RequestContext.userId());
            agentTaskRunMapper.updateById(run);

            auditLogRecordService.record(
                    "AGENT",
                    "TASK_EXECUTE_SUCCESS",
                    "AGENT_TASK_RUN",
                    String.valueOf(run.getId()),
                    normalizedTaskCode,
                    null,
                    toJson(Map.of("taskCode", normalizedTaskCode, "status", "SUCCESS"))
            );

            return runToDetail(run, steps, result);
        } catch (Exception ex) {
            if (steps.isEmpty() || !"FAILED".equals(String.valueOf(steps.get(steps.size() - 1).get("status")))) {
                steps.add(stepRecord(steps.size() + 1, "error", "执行失败", "FAILED", ex.getMessage(), 0L, null));
            }
            run.setStatus("FAILED");
            run.setStepsJson(toJson(steps));
            run.setResultJson(toJson(Map.of()));
            run.setErrorMessage(limit(ex.getMessage(), 900));
            run.setFinishedAt(LocalDateTime.now());
            run.setUpdatedBy(RequestContext.userId());
            agentTaskRunMapper.updateById(run);

            auditLogRecordService.record(
                    "AGENT",
                    "TASK_EXECUTE_FAILED",
                    "AGENT_TASK_RUN",
                    String.valueOf(run.getId()),
                    normalizedTaskCode,
                    null,
                    toJson(Map.of("taskCode", normalizedTaskCode, "status", "FAILED", "error", limit(ex.getMessage(), 300)))
            );

            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Agent 任务执行失败: " + ex.getMessage());
        }
    }

    public Map<String, Object> listRuns(Integer pageNo, Integer pageSize, String taskCode, String status) {
        int safePageNo = Math.max(1, pageNo == null ? 1 : pageNo);
        int safePageSize = Math.max(1, Math.min(100, pageSize == null ? 20 : pageSize));
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<AgentTaskRunEntity> countQw = runFilter(taskCode, status);
        Long total = agentTaskRunMapper.selectCount(countQw);

        LambdaQueryWrapper<AgentTaskRunEntity> listQw = runFilter(taskCode, status)
                .orderByDesc(AgentTaskRunEntity::getStartedAt)
                .orderByDesc(AgentTaskRunEntity::getId)
                .last("limit " + offset + "," + safePageSize);

        List<Map<String, Object>> list = agentTaskRunMapper.selectList(listQw).stream()
                .map(this::runToSimple)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total == null ? 0 : total);
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        return result;
    }

    public Map<String, Object> runDetail(Long id) {
        AgentTaskRunEntity run = agentTaskRunMapper.selectOne(new LambdaQueryWrapper<AgentTaskRunEntity>()
                .eq(AgentTaskRunEntity::getTenantId, RequestContext.tenantId())
                .eq(AgentTaskRunEntity::getId, id));
        if (run == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Agent 执行记录不存在");
        }
        return runToDetail(run, parseJsonList(run.getStepsJson()), parseJsonMap(run.getResultJson()));
    }

    private Map<String, Object> runReplenishSuggestion(Map<String, Object> target,
                                                        List<Map<String, Object>> steps) throws Exception {
        Map<String, Object> params = doStep(steps, "target", "接收任务目标", () -> {
            int defaultLimit = knowledgeService.intRule("AGENT_RESULT_DISPLAY_THRESHOLD", 5);
            int limit = readInt(target.get("limit"), defaultLimit, 1, 20);
            BigDecimal lowStockThreshold = readDecimal(target.get("lowStockThreshold"), knowledgeService.decimalRule("LOW_STOCK_DEFAULT_THRESHOLD", BigDecimal.TEN));
            return Map.of("limit", limit, "lowStockThreshold", lowStockThreshold);
        }, p -> "参数接收完成");

        int limit = ((Number) params.get("limit")).intValue();
        BigDecimal threshold = (BigDecimal) params.get("lowStockThreshold");

        List<InventoryEntity> lowRows = doStep(steps, "read", "读取低库存", () ->
                        inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                                .le(InventoryEntity::getAvailableQty, threshold)
                                .orderByAsc(InventoryEntity::getAvailableQty)
                                .last("limit 300")),
                rows -> "低库存记录 " + rows.size() + " 条");

        Set<Long> productIds = lowRows.stream().map(InventoryEntity::getProductId).collect(Collectors.toSet());
        Map<Long, ProductEntity> productMap = doStep(steps, "read", "读取商品主数据", () -> {
            if (productIds.isEmpty()) {
                return Map.<Long, ProductEntity>of();
            }
            return productMapper.selectList(new LambdaQueryWrapper<ProductEntity>()
                            .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                            .in(ProductEntity::getId, productIds))
                    .stream()
                    .collect(Collectors.toMap(ProductEntity::getId, it -> it));
        }, map -> "命中商品 " + map.size() + " 条");

        return doStep(steps, "analyze", "生成补货建议", () -> {
            List<Map<String, Object>> suggestions = lowRows.stream().map(row -> {
                BigDecimal available = row.getAvailableQty() == null ? BigDecimal.ZERO : row.getAvailableQty();
                BigDecimal suggestQty = threshold.subtract(available);
                if (suggestQty.compareTo(BigDecimal.ZERO) < 0) {
                    suggestQty = BigDecimal.ZERO;
                }
                ProductEntity product = productMap.get(row.getProductId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("productId", row.getProductId());
                item.put("productCode", product == null ? null : product.getProductCode());
                item.put("productName", product == null ? null : product.getProductName());
                item.put("availableQty", available);
                item.put("suggestReplenishQty", suggestQty.setScale(2, RoundingMode.HALF_UP));
                item.put("reason", "低库存触发补货");
                return item;
            }).limit(limit).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskCode", TASK_REPLENISH);
            result.put("taskName", taskNameByCode(TASK_REPLENISH));
            result.put("summary", "已输出 " + suggestions.size() + " 条补货建议");
            result.put("metrics", Map.of("lowStockCount", lowRows.size(), "suggestionCount", suggestions.size(), "threshold", threshold));
            result.put("suggestions", suggestions);
            result.put("businessBasis", knowledgeService.matchKnowledge("低库存补货", "inventory"));
            return result;
        }, data -> "补货建议生成完成");
    }

    private Map<String, Object> runLowStockAnalysis(Map<String, Object> target,
                                                    List<Map<String, Object>> steps) throws Exception {
        int limit = doStep(steps, "target", "接收输出条数", () ->
                        readInt(target.get("limit"), knowledgeService.intRule("AGENT_RESULT_DISPLAY_THRESHOLD", 5), 1, 20),
                value -> "输出条数: " + value);
        int recentDays = doStep(steps, "target", "接收趋势窗口", () ->
                        readInt(target.get("recentDays"), 7, 1, 30),
                value -> "趋势窗口: " + value + " 天");

        List<InventoryEntity> rows = doStep(steps, "read", "读取库存与低库存记录", () ->
                        inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                                .orderByAsc(InventoryEntity::getAvailableQty)
                                .last("limit 500")),
                value -> "库存记录 " + value.size() + " 条");

        Set<Long> productIds = rows.stream().map(InventoryEntity::getProductId).collect(Collectors.toSet());
        Map<Long, ProductEntity> productMap = doStep(steps, "read", "读取商品主数据", () -> {
            if (productIds.isEmpty()) {
                return Map.<Long, ProductEntity>of();
            }
            return productMapper.selectList(new LambdaQueryWrapper<ProductEntity>()
                            .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                            .in(ProductEntity::getId, productIds))
                    .stream()
                    .collect(Collectors.toMap(ProductEntity::getId, it -> it));
        }, value -> "商品记录 " + value.size() + " 条");

        LocalDateTime since = LocalDateTime.now().minusDays(recentDays);
        Long outboundCount = doStep(steps, "read", "读取近期出库趋势", () ->
                        outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                                .ge(OutboundOrderEntity::getCreatedAt, since)),
                value -> "近 " + recentDays + " 天出库单 " + value + " 条");

        return doStep(steps, "analyze", "生成低库存分析结果", () -> {
            List<Map<String, Object>> top = rows.stream().limit(limit).map(item -> {
                ProductEntity product = productMap.get(item.getProductId());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productId", item.getProductId());
                row.put("productCode", product == null ? null : product.getProductCode());
                row.put("productName", product == null ? null : product.getProductName());
                row.put("availableQty", item.getAvailableQty());
                row.put("riskLevel", item.getAvailableQty() != null && item.getAvailableQty().compareTo(BigDecimal.ZERO) <= 0 ? "HIGH" : "MEDIUM");
                row.put("suggestAction", "优先检查在途/补货计划并确认安全库存阈值");
                return row;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskCode", TASK_LOW_STOCK);
            result.put("taskName", taskNameByCode(TASK_LOW_STOCK));
            result.put("summary", "已输出低库存分析 " + top.size() + " 条");
            result.put("basis", Map.of("inventoryCount", rows.size(), "recentDays", recentDays, "recentOutboundOrders", outboundCount == null ? 0 : outboundCount));
            result.put("analysisList", top);
            result.put("businessBasis", knowledgeService.matchKnowledge("低库存分析", "inventory"));
            return result;
        }, value -> "低库存分析完成");
    }

    private Map<String, Object> runAnomalyPatrol(Map<String, Object> target,
                                                  List<Map<String, Object>> steps) throws Exception {
        int staleDays = readInt(target.get("staleDays"), 3, 1, 30);
        BigDecimal threshold = readDecimal(target.get("lowStockThreshold"), knowledgeService.decimalRule("LOW_STOCK_DEFAULT_THRESHOLD", BigDecimal.TEN));

        List<InventoryEntity> negative = doStep(steps, "read", "巡检负库存", () ->
                        inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                                .lt(InventoryEntity::getAvailableQty, BigDecimal.ZERO)
                                .last("limit 200")),
                rows -> "负库存记录 " + rows.size() + " 条");

        List<InventoryEntity> lowStock = doStep(steps, "read", "巡检低库存", () ->
                        inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                                .le(InventoryEntity::getAvailableQty, threshold)
                                .orderByAsc(InventoryEntity::getAvailableQty)
                                .last("limit 300")),
                rows -> "低库存记录 " + rows.size() + " 条");

        LocalDateTime before = LocalDateTime.now().minusDays(staleDays);
        Long inboundUnfinished = doStep(steps, "read", "巡检未完成入库单", () ->
                        inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                                .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                                .in(InboundOrderEntity::getStatus, List.of("DRAFT", "SUBMITTED", "APPROVED"))
                                .le(InboundOrderEntity::getCreatedAt, before)),
                count -> "未完成入库单 " + count + " 条");

        Long outboundUnfinished = doStep(steps, "read", "巡检未完成出库单", () ->
                        outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                                .in(OutboundOrderEntity::getStatus, List.of("DRAFT", "SUBMITTED", "APPROVED"))
                                .le(OutboundOrderEntity::getCreatedAt, before)),
                count -> "未完成出库单 " + count + " 条");

        return doStep(steps, "analyze", "汇总异常", () -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskCode", TASK_ANOMALY);
            result.put("taskName", taskNameByCode(TASK_ANOMALY));
            result.put("summary", "异常巡检完成");
            result.put("metrics", Map.of(
                    "negativeInventoryCount", negative.size(),
                    "lowStockCount", lowStock.size(),
                    "inboundUnfinished", inboundUnfinished == null ? 0 : inboundUnfinished,
                    "outboundUnfinished", outboundUnfinished == null ? 0 : outboundUnfinished
            ));
            result.put("negativeInventory", negative.stream().limit(20).map(this::inventorySimple).toList());
            result.put("lowStock", lowStock.stream().limit(20).map(this::inventorySimple).toList());
            result.put("businessBasis", knowledgeService.matchKnowledge("异常巡检 低库存", "warehouse"));
            return result;
        }, data -> "异常汇总完成");
    }

    private Map<String, Object> runDailyOpsReport(Map<String, Object> target,
                                                   List<Map<String, Object>> steps) throws Exception {
        LocalDate reportDate = doStep(steps, "target", "接收日报日期", () -> {
            String dateText = readString(target.get("date"));
            return StringUtils.hasText(dateText) ? LocalDate.parse(dateText) : LocalDate.now();
        }, date -> "日报日期: " + date);

        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Map<String, Object> inboundStat = doStep(steps, "read", "统计今日入库", () -> Map.of(
                "created", inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                        .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                        .ge(InboundOrderEntity::getCreatedAt, start)
                        .lt(InboundOrderEntity::getCreatedAt, end)),
                "posted", inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                        .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                        .eq(InboundOrderEntity::getStatus, "POSTED")
                        .ge(InboundOrderEntity::getCreatedAt, start)
                        .lt(InboundOrderEntity::getCreatedAt, end))
        ), data -> "入库统计完成");

        Map<String, Object> outboundStat = doStep(steps, "read", "统计今日出库", () -> Map.of(
                "created", outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                        .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                        .ge(OutboundOrderEntity::getCreatedAt, start)
                        .lt(OutboundOrderEntity::getCreatedAt, end)),
                "shipped", outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                        .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                        .eq(OutboundOrderEntity::getStatus, "SHIPPED")
                        .ge(OutboundOrderEntity::getCreatedAt, start)
                        .lt(OutboundOrderEntity::getCreatedAt, end))
        ), data -> "出库统计完成");

        Map<String, Object> ticketStat = doStep(steps, "read", "统计工单状态", () -> Map.of(
                "todayTickets", customerServiceTicketMapper.selectCount(new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                        .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId())
                        .ge(CustomerServiceTicketEntity::getCreatedAt, start)
                        .lt(CustomerServiceTicketEntity::getCreatedAt, end)),
                "openTickets", customerServiceTicketMapper.selectCount(new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                        .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId())
                        .eq(CustomerServiceTicketEntity::getStatus, "OPEN")),
                "processingTickets", customerServiceTicketMapper.selectCount(new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                        .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId())
                        .eq(CustomerServiceTicketEntity::getStatus, "PROCESSING"))
        ), data -> "工单统计完成");

        Long lowStockCount = doStep(steps, "read", "统计低库存", () ->
                        inventoryMapper.selectCount(new LambdaQueryWrapper<InventoryEntity>()
                                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                                .le(InventoryEntity::getAvailableQty, BigDecimal.TEN)),
                count -> "低库存数量: " + count);

        return doStep(steps, "analyze", "生成运营日报", () -> {
            String reportText = String.format(
                    Locale.ROOT,
                    "运营日报(%s)：入库 %s（过账 %s），出库 %s（发运 %s），低库存 %s，今日工单 %s，处理中工单 %s。",
                    reportDate,
                    inboundStat.get("created"),
                    inboundStat.get("posted"),
                    outboundStat.get("created"),
                    outboundStat.get("shipped"),
                    lowStockCount == null ? 0 : lowStockCount,
                    ticketStat.get("todayTickets"),
                    ticketStat.get("processingTickets")
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskCode", TASK_DAILY);
            result.put("taskName", taskNameByCode(TASK_DAILY));
            result.put("reportDate", reportDate.toString());
            result.put("reportText", reportText);
            result.put("sections", Map.of(
                    "inbound", inboundStat,
                    "outbound", outboundStat,
                    "tickets", ticketStat,
                    "lowStock", Map.of("count", lowStockCount == null ? 0 : lowStockCount)
            ));
            return result;
        }, data -> "日报生成完成");
    }

    private Map<String, Object> runTicketTriage(Map<String, Object> target,
                                                List<Map<String, Object>> steps) throws Exception {
        int limit = doStep(steps, "target", "接收输出条数", () ->
                        readInt(target.get("limit"), 5, 1, 20),
                value -> "输出条数: " + value);

        List<CustomerServiceTicketEntity> tickets = doStep(steps, "read", "读取待处理工单", () ->
                        customerServiceTicketMapper.selectList(new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                                .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId())
                                .in(CustomerServiceTicketEntity::getStatus, List.of("OPEN", "PROCESSING"))
                                .orderByDesc(CustomerServiceTicketEntity::getCreatedAt)
                                .last("limit 200")),
                value -> "待处理工单 " + value.size() + " 条");

        List<FAQKnowledgeEntity> faqList = doStep(steps, "read", "读取FAQ知识库", () ->
                        faqKnowledgeMapper.selectList(new LambdaQueryWrapper<FAQKnowledgeEntity>()
                                .eq(FAQKnowledgeEntity::getTenantId, RequestContext.tenantId())
                                .eq(FAQKnowledgeEntity::getEnabled, 1)
                                .orderByDesc(FAQKnowledgeEntity::getPriority)
                                .last("limit 100")),
                value -> "FAQ " + value.size() + " 条");

        return doStep(steps, "analyze", "生成工单处理建议", () -> {
            List<Map<String, Object>> suggestions = tickets.stream().limit(limit).map(ticket -> {
                String content = ticket.getContent() == null ? "" : ticket.getContent();
                String category = classifyTicket(content);
                String priority = suggestPriority(content, ticket.getPriority());
                List<Map<String, Object>> knowledgeRefs = knowledgeService.matchKnowledge(content, "customer-service");
                String faqHit = faqList.stream()
                        .filter(faq -> content.contains(String.valueOf(faq.getQuestion()).replace("？", "").trim()))
                        .map(FAQKnowledgeEntity::getQuestion)
                        .findFirst()
                        .orElse("");

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ticketId", ticket.getId());
                row.put("ticketNo", ticket.getTicketNo());
                row.put("categorySuggestion", category);
                row.put("prioritySuggestion", priority);
                row.put("faqHitSuggestion", faqHit.isBlank() ? "未命中，建议补充FAQ关键词" : faqHit);
                row.put("replySuggestion", buildReplySuggestion(category));
                row.put("sopSuggestion", buildSopSuggestion(category));
                row.put("knowledgeRefs", knowledgeRefs);
                row.put("knowledgeFallbackNotice", knowledgeRefs.isEmpty() ? "未命中知识库，当前建议来自规则回退。" : "");
                return row;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskCode", TASK_TICKET_TRIAGE);
            result.put("taskName", taskNameByCode(TASK_TICKET_TRIAGE));
            result.put("summary", "已生成工单处理建议 " + suggestions.size() + " 条");
            result.put("suggestions", suggestions);
            result.put("businessBasis", knowledgeService.matchKnowledge("工单处理建议", "customer-service"));
            return result;
        }, value -> "工单处理建议生成完成");
    }

    private Map<String, Object> runSopRecommendation(Map<String, Object> target,
                                                     List<Map<String, Object>> steps) throws Exception {
        String topic = doStep(steps, "target", "接收SOP主题", () -> {
            String value = readString(target.get("topic"));
            return StringUtils.hasText(value) ? value.trim() : "通用客服处理";
        }, value -> "主题: " + value);

        List<FAQKnowledgeEntity> faqList = doStep(steps, "read", "读取相关FAQ", () ->
                        faqKnowledgeMapper.selectList(new LambdaQueryWrapper<FAQKnowledgeEntity>()
                                .eq(FAQKnowledgeEntity::getTenantId, RequestContext.tenantId())
                                .eq(FAQKnowledgeEntity::getEnabled, 1)
                                .orderByDesc(FAQKnowledgeEntity::getPriority)
                                .last("limit 100")),
                value -> "FAQ " + value.size() + " 条");

        return doStep(steps, "analyze", "生成SOP建议", () -> {
            List<String> stepsList = buildSopSteps(topic);
            List<String> faqRefs = faqList.stream()
                    .map(FAQKnowledgeEntity::getQuestion)
                    .filter(question -> question != null && (question.contains(topic) || topic.contains("通用")))
                    .limit(3)
                    .toList();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskCode", TASK_SOP);
            result.put("taskName", taskNameByCode(TASK_SOP));
            result.put("topic", topic);
            result.put("summary", "已生成 \"" + topic + "\" 的SOP建议");
            result.put("steps", stepsList);
            result.put("risks", List.of("超时未响应", "信息不完整导致误判", "跨部门协同延迟"));
            result.put("reviewChecks", List.of("是否确认客户诉求", "是否完成工单留痕", "是否同步下一步时间点"));
            result.put("faqReferences", faqRefs);
            result.put("businessBasis", knowledgeService.matchKnowledge(topic, "customer-service"));
            return result;
        }, value -> "SOP建议生成完成");
    }

    private <T> T doStep(List<Map<String, Object>> steps,
                         String phase,
                         String name,
                         StepSupplier<T> supplier,
                         java.util.function.Function<T, String> detailBuilder) throws Exception {
        long start = System.currentTimeMillis();
        try {
            T data = supplier.get();
            long duration = System.currentTimeMillis() - start;
            steps.add(stepRecord(steps.size() + 1, phase, name, "SUCCESS", detailBuilder.apply(data), duration, snapshot(data)));
            return data;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            steps.add(stepRecord(steps.size() + 1, phase, name, "FAILED", ex.getMessage(), duration, null));
            throw ex;
        }
    }

    private Map<String, Object> runToSimple(AgentTaskRunEntity run) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(run.getId()));
        row.put("taskCode", run.getTaskCode());
        row.put("taskName", run.getTaskName());
        row.put("status", run.getStatus());
        row.put("startedAt", run.getStartedAt());
        row.put("finishedAt", run.getFinishedAt());
        row.put("errorMessage", run.getErrorMessage());
        return row;
    }

    private Map<String, Object> runToDetail(AgentTaskRunEntity run,
                                            List<Map<String, Object>> steps,
                                            Map<String, Object> result) {
        Map<String, Object> detail = runToSimple(run);
        detail.put("target", parseJsonMap(run.getTargetJson()));
        detail.put("steps", steps);
        detail.put("result", result);
        return detail;
    }

    private LambdaQueryWrapper<AgentTaskRunEntity> runFilter(String taskCode, String status) {
        LambdaQueryWrapper<AgentTaskRunEntity> qw = new LambdaQueryWrapper<AgentTaskRunEntity>()
                .eq(AgentTaskRunEntity::getTenantId, RequestContext.tenantId());
        if (StringUtils.hasText(taskCode)) {
            qw.eq(AgentTaskRunEntity::getTaskCode, normalizeTaskCode(taskCode));
        }
        if (StringUtils.hasText(status)) {
            qw.eq(AgentTaskRunEntity::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        return qw;
    }

    private Map<String, Object> taskInfo(String code,
                                         String name,
                                         String description,
                                         String intro,
                                         List<String> readData,
                                         String output,
                                         List<Map<String, Object>> params) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskCode", code);
        task.put("taskName", name);
        task.put("description", description);
        task.put("intro", intro);
        task.put("readData", readData);
        task.put("output", output);
        task.put("params", params);
        return task;
    }

    private Map<String, Object> taskParam(String key, String label, String description, String type, Object defaultValue) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("key", key);
        param.put("label", label);
        param.put("description", description);
        param.put("type", type);
        param.put("defaultValue", defaultValue);
        return param;
    }

    private String taskNameByCode(String taskCode) {
        return switch (taskCode) {
            case TASK_LOW_STOCK -> "低库存分析";
            case TASK_REPLENISH -> "补货建议 Agent";
            case TASK_ANOMALY -> "异常巡检 Agent";
            case TASK_DAILY -> "运营日报 Agent";
            case TASK_TICKET_TRIAGE -> "工单处理建议";
            case TASK_SOP -> "SOP 建议";
            default -> throw new BizException(ErrorCode.BIZ_ERROR.code(), "不支持的任务类型");
        };
    }

    private String normalizeTaskCode(String taskCode) {
        if (!StringUtils.hasText(taskCode)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "taskCode 不能为空");
        }
        return taskCode.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> stepRecord(int stepNo,
                                           String phase,
                                           String name,
                                           String status,
                                           String detail,
                                           long durationMs,
                                           Object stepSnapshot) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("stepNo", stepNo);
        step.put("phase", phase);
        step.put("name", name);
        step.put("status", status);
        step.put("detail", detail);
        step.put("durationMs", durationMs);
        step.put("snapshot", stepSnapshot);
        step.put("timestamp", LocalDateTime.now());
        return step;
    }

    private Object snapshot(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof List<?> list) {
            return Map.of("type", "LIST", "size", list.size(), "sample", list.stream().limit(3).toList());
        }
        if (data instanceof Map<?, ?> map) {
            return Map.of("type", "MAP", "size", map.size(), "keys", map.keySet().stream().limit(8).toList());
        }
        return data;
    }

    private Map<String, Object> inventorySimple(InventoryEntity item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("inventoryId", item.getId());
        row.put("warehouseId", item.getWarehouseId());
        row.put("locationId", item.getLocationId());
        row.put("productId", item.getProductId());
        row.put("availableQty", item.getAvailableQty());
        return row;
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> parseJsonMap(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of("_raw", raw);
        }
    }

    private List<Map<String, Object>> parseJsonList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return List.of(Map.of("status", "FAILED", "detail", "步骤解析失败", "raw", raw));
        }
    }

    private int readInt(Object value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(value));
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private BigDecimal readDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String readString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String limit(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String classifyTicket(String content) {
        String text = content == null ? "" : content.toLowerCase(Locale.ROOT);
        if (text.contains("发票")) {
            return "发票/财务";
        }
        if (text.contains("退") || text.contains("换")) {
            return "退换货";
        }
        if (text.contains("质量") || text.contains("损坏")) {
            return "质量反馈";
        }
        if (text.contains("物流") || text.contains("发货") || text.contains("催")) {
            return "物流催发";
        }
        return "通用咨询";
    }

    private String suggestPriority(String content, String existingPriority) {
        String text = content == null ? "" : content.toLowerCase(Locale.ROOT);
        if (text.contains("紧急") || text.contains("立刻") || text.contains("升级")) {
            return "HIGH";
        }
        if (existingPriority != null && !existingPriority.isBlank()) {
            return existingPriority.toUpperCase(Locale.ROOT);
        }
        return "MEDIUM";
    }

    private String buildReplySuggestion(String category) {
        return switch (category) {
            case "物流催发" -> "建议回复：已加急核查出库与物流状态，将在30分钟内同步最新进展。";
            case "退换货" -> "建议回复：已为您创建退换货工单，请先提供订单号与问题商品照片。";
            case "质量反馈" -> "建议回复：已记录质量问题并升级质检，请先协助收集批次与问题描述。";
            case "发票/财务" -> "建议回复：已转财务同事处理发票信息，将在一个工作日内反馈。";
            default -> "建议回复：已收到您的问题，我们会尽快核查并同步处理进度。";
        };
    }

    private String buildSopSuggestion(String category) {
        return switch (category) {
            case "物流催发" -> "SOP：核单 -> 查库存与单据状态 -> 物流催发 -> 回访确认。";
            case "退换货" -> "SOP：核验条件 -> 创建售后工单 -> 指导寄回/上门 -> 结果回访。";
            case "质量反馈" -> "SOP：收集证据 -> 提交质检 -> 判责方案 -> 客户补偿沟通。";
            case "发票/财务" -> "SOP：核对抬头税号 -> 提交财务 -> 开票回传 -> 留痕归档。";
            default -> "SOP：确认诉求 -> 分类分派 -> 跟进处理 -> 结果回访。";
        };
    }

    private List<String> buildSopSteps(String topic) {
        String normalized = topic == null ? "" : topic.toLowerCase(Locale.ROOT);
        if (normalized.contains("物流") || normalized.contains("催发")) {
            return List.of("确认订单与发运状态", "核对库存与出库单进度", "联系仓配加急处理", "向客户反馈时间承诺并跟进回访");
        }
        if (normalized.contains("退") || normalized.contains("换")) {
            return List.of("确认退换货条件", "登记售后工单与责任人", "安排逆向物流或上门取件", "验收入库后完成退款/换货并回访");
        }
        if (normalized.contains("库存")) {
            return List.of("确认库存事实与阈值", "排查在途与待入库单据", "输出补货或调拨建议", "安排复盘并更新安全库存口径");
        }
        return List.of("确认问题场景与影响范围", "匹配标准处理模板", "执行处理并留痕", "复盘结果并更新FAQ/SOP");
    }

    @FunctionalInterface
    private interface StepSupplier<T> {
        T get() throws Exception;
    }
}
