package com.novadepot.backend.modules.ai.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RuleAiProvider implements AiProvider {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final BigDecimal lowStockThreshold;
    private final BigDecimal abnormalChangeThreshold;

    public RuleAiProvider(InventoryMapper inventoryMapper,
                          InventoryTransactionMapper inventoryTransactionMapper,
                          @Value("${app.ai.rule.low-stock-threshold:10}") BigDecimal lowStockThreshold,
                          @Value("${app.ai.rule.abnormal-change-threshold:100}") BigDecimal abnormalChangeThreshold) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.lowStockThreshold = lowStockThreshold;
        this.abnormalChangeThreshold = abnormalChangeThreshold;
    }

    @Override
    public String providerName() {
        return "rule";
    }

    @Override
    public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (hasAny(normalized, "库存", "stock") && hasAny(normalized, "多少", "查询", "总量", "概览", "summary")) {
            return inventoryOverview(scene);
        }
        if (hasAny(normalized, "低库存", "预警", "不足")) {
            return lowStockAnalysis(scene);
        }
        if (hasAny(normalized, "补货", "采购建议", "补仓")) {
            return replenishSuggestion(scene);
        }
        if (hasAny(normalized, "异常", "波动", "突增", "突减")) {
            return abnormalInventoryHint(scene);
        }
        if (hasAny(normalized, "日报", "日报表", "今日总结")) {
            return periodicReport(scene, 1, "日报");
        }
        if (hasAny(normalized, "周报", "每周", "7天")) {
            return periodicReport(scene, 7, "周报");
        }
        if (hasAny(normalized, "sop", "流程", "作业", "操作规范", "标准流程")) {
            return sopAnswer(scene, normalized);
        }
        if (hasAny(normalized, "经营", "利润", "策略", "企业建议", "业务建议")) {
            return enterpriseAdvice(scene);
        }

        return Map.of(
                "reply", "已识别到这是通用问题。当前免费阶段建议提问：库存概览、低库存分析、补货建议、异常库存、日报/周报、SOP。",
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.72
        );
    }

    private Map<String, Object> inventoryOverview(String scene) {
        List<InventoryEntity> rows = inventoryRows();
        BigDecimal totalAvailable = rows.stream()
                .map(InventoryEntity::getAvailableQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String reply = String.format(
                "库存概览：共 %d 个库存点，可用库存合计 %s。建议优先关注低于阈值 %s 的SKU。",
                rows.size(),
                totalAvailable.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                lowStockThreshold.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.91,
                "metrics", Map.of(
                        "inventoryPoints", rows.size(),
                        "totalAvailable", totalAvailable,
                        "lowStockThreshold", lowStockThreshold
                )
        );
    }

    private Map<String, Object> lowStockAnalysis(String scene) {
        List<InventoryEntity> lowRows = inventoryRows().stream()
                .filter(i -> i.getAvailableQty() != null && i.getAvailableQty().compareTo(lowStockThreshold) <= 0)
                .sorted(Comparator.comparing(InventoryEntity::getAvailableQty))
                .limit(5)
                .toList();

        String items = lowRows.stream()
                .map(i -> "商品" + i.getProductId() + "(可用:" + i.getAvailableQty().stripTrailingZeros().toPlainString() + ")")
                .collect(Collectors.joining("，"));

        String reply = lowRows.isEmpty()
                ? "当前未发现低库存SKU，可继续按日巡检库存阈值。"
                : "低库存分析完成，Top风险SKU：" + items + "。建议优先补货并核查在途库存。";

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", lowRows.isEmpty() ? 0.88 : 0.93,
                "metrics", Map.of(
                        "lowStockCount", lowRows.size(),
                        "threshold", lowStockThreshold
                )
        );
    }

    private Map<String, Object> replenishSuggestion(String scene) {
        List<Map<String, Object>> suggestions = inventoryRows().stream()
                .filter(i -> i.getAvailableQty() != null && i.getAvailableQty().compareTo(lowStockThreshold) < 0)
                .sorted(Comparator.comparing(InventoryEntity::getAvailableQty))
                .limit(5)
                .map(i -> {
                    BigDecimal shortage = lowStockThreshold.subtract(i.getAvailableQty()).max(BigDecimal.ZERO);
                    return Map.<String, Object>of(
                            "productId", i.getProductId(),
                            "warehouseId", i.getWarehouseId(),
                            "locationId", i.getLocationId(),
                            "currentQty", i.getAvailableQty(),
                            "suggestReplenishQty", shortage
                    );
                })
                .toList();

        String reply = suggestions.isEmpty()
                ? "当前库存充足，无需立即补货。"
                : "补货建议已生成：优先按建议补货量处理前5个低库存SKU。";

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.89,
                "suggestions", suggestions
        );
    }

    private Map<String, Object> abnormalInventoryHint(String scene) {
        LocalDateTime since = LocalDateTime.now().minusDays(3);
        List<InventoryTransactionEntity> txns = inventoryTransactionMapper.selectList(new LambdaQueryWrapper<InventoryTransactionEntity>()
                .eq(InventoryTransactionEntity::getTenantId, RequestContext.tenantId())
                .ge(InventoryTransactionEntity::getOccurredAt, since)
                .orderByDesc(InventoryTransactionEntity::getOccurredAt)
                .last("limit 200"));

        List<InventoryTransactionEntity> abnormal = txns.stream()
                .filter(t -> t.getChangeQty() != null && t.getChangeQty().abs().compareTo(abnormalChangeThreshold) >= 0)
                .limit(5)
                .toList();

        String detail = abnormal.stream()
                .map(t -> t.getBizNo() + "/商品" + t.getProductId() + " 变动" + t.getChangeQty().stripTrailingZeros().toPlainString())
                .collect(Collectors.joining("；"));

        String reply = abnormal.isEmpty()
                ? "近3天未发现明显异常库存波动。"
                : "检测到疑似异常库存波动：" + detail + "。建议复核对应单据与操作人。";

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", abnormal.isEmpty() ? 0.82 : 0.9,
                "metrics", Map.of(
                        "abnormalTxnCount", abnormal.size(),
                        "windowDays", 3,
                        "threshold", abnormalChangeThreshold
                )
        );
    }

    private Map<String, Object> periodicReport(String scene, int days, String title) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<InventoryTransactionEntity> txns = inventoryTransactionMapper.selectList(new LambdaQueryWrapper<InventoryTransactionEntity>()
                .eq(InventoryTransactionEntity::getTenantId, RequestContext.tenantId())
                .ge(InventoryTransactionEntity::getOccurredAt, since));

        BigDecimal inboundQty = txns.stream()
                .filter(t -> t.getChangeQty() != null && t.getChangeQty().compareTo(BigDecimal.ZERO) > 0)
                .map(InventoryTransactionEntity::getChangeQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outboundQty = txns.stream()
                .filter(t -> t.getChangeQty() != null && t.getChangeQty().compareTo(BigDecimal.ZERO) < 0)
                .map(t -> t.getChangeQty().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal net = inboundQty.subtract(outboundQty);
        String reply = String.format(
                "%s（%s ~ %s）：入库 %s，出库 %s，净变化 %s。建议关注高频出库SKU与低库存联动。",
                title,
                since.format(TIME_FMT),
                LocalDateTime.now().format(TIME_FMT),
                inboundQty.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                outboundQty.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                net.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.9,
                "metrics", Map.of(
                        "windowDays", days,
                        "txnCount", txns.size(),
                        "inboundQty", inboundQty,
                        "outboundQty", outboundQty,
                        "netChange", net
                )
        );
    }

    private Map<String, Object> sopAnswer(String scene, String normalized) {
        String sop;
        if (hasAny(normalized, "入库")) {
            sop = "入库SOP：到货核对 -> 质检 -> 上架分配库位 -> 入账 -> 抽检复核。";
        } else if (hasAny(normalized, "出库")) {
            sop = "出库SOP：拣货波次 -> 复核 -> 打包 -> 发运登记 -> 回写出库单。";
        } else if (hasAny(normalized, "盘点")) {
            sop = "盘点SOP：冻结范围 -> 现场盘点 -> 差异复核 -> 审批调整 -> 盘点归档。";
        } else {
            sop = "通用SOP建议：明确责任人、标准步骤、复核节点与异常升级路径。";
        }

        return Map.of(
                "reply", sop,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.87
        );
    }

    private Map<String, Object> enterpriseAdvice(String scene) {
        List<InventoryEntity> rows = inventoryRows();
        long lowCount = rows.stream()
                .filter(i -> i.getAvailableQty() != null && i.getAvailableQty().compareTo(lowStockThreshold) <= 0)
                .count();

        BigDecimal ratio = rows.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(lowCount)
                .divide(BigDecimal.valueOf(rows.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<String> advices = new ArrayList<>();
        if (ratio.compareTo(BigDecimal.valueOf(20)) >= 0) {
            advices.add("低库存占比较高，建议先补齐高周转SKU安全库存。");
        }
        advices.add("建立采购补货周期与销量联动阈值，减少缺货与积压并存。");
        advices.add("按仓库维度监控出入库峰值，优化人力与波次策略。");

        String reply = "经营建议：" + String.join(" ", advices);

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.79,
                "metrics", Map.of(
                        "lowStockRatioPct", ratio,
                        "inventoryPoints", rows.size()
                )
        );
    }

    private List<InventoryEntity> inventoryRows() {
        return inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                .last("limit 500"));
    }

    private boolean hasAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
