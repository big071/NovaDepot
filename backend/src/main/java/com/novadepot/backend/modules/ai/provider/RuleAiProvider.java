package com.novadepot.backend.modules.ai.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.modules.inventory.LowStockPolicyService;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RuleAiProvider implements AiProvider {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final LowStockPolicyService lowStockPolicyService;
    private final BigDecimal lowStockThreshold;
    private final BigDecimal abnormalChangeThreshold;

    public RuleAiProvider(InventoryMapper inventoryMapper,
                          InventoryTransactionMapper inventoryTransactionMapper,
                          LowStockPolicyService lowStockPolicyService,
                          @Value("${app.ai.rule.low-stock-threshold:10}") BigDecimal lowStockThreshold,
                          @Value("${app.ai.rule.abnormal-change-threshold:100}") BigDecimal abnormalChangeThreshold) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.lowStockPolicyService = lowStockPolicyService;
        this.lowStockThreshold = lowStockThreshold;
        this.abnormalChangeThreshold = abnormalChangeThreshold;
    }

    @Override
    public String providerName() {
        return "rule";
    }

    @Override
    public AiProviderResponse chat(String scene, String message, Map<String, Object> context) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (hasAny(normalized, "\u4f4e\u5e93\u5b58", "\u5e93\u5b58\u4e0d\u8db3", "\u5e93\u5b58\u9884\u8b66")) {
            return response(lowStockAnalysis(scene));
        }
        if (hasAny(normalized, "\u5e93\u5b58") && hasAny(normalized, "\u591a\u5c11", "\u67e5\u8be2", "\u603b\u91cf", "\u6982\u89c8", "\u60c5\u51b5", "\u600e\u4e48\u6837")) {
            return response(inventoryOverview(scene));
        }
        if (hasAny(normalized, "\u8865\u8d27", "\u91c7\u8d2d\u5efa\u8bae", "\u8865\u4ed3")) {
            return response(replenishSuggestion(scene));
        }

        if (hasAny(normalized, "低库存", "库存不足", "库存预警")) {
            return response(lowStockAnalysis(scene));
        }
        if (hasAny(normalized, "库存") && hasAny(normalized, "多少", "查询", "总量", "概览", "情况", "怎么样")) {
            return response(inventoryOverview(scene));
        }
        if (hasAny(normalized, "补货", "采购建议", "补仓")) {
            return response(replenishSuggestion(scene));
        }
        if (hasAny(normalized, "异常", "波动", "突增", "突减")) {
            return response(abnormalInventoryHint(scene));
        }
        if (hasAny(normalized, "今天", "日报", "今日总结")) {
            return response(periodicReport(scene, 1, "日报"));
        }

        if (hasAny(normalized, "库存", "stock") && hasAny(normalized, "多少", "查询", "总量", "概览", "summary")) {
            return response(inventoryOverview(scene));
        }
        if (hasAny(normalized, "低库存", "预警", "不足")) {
            return response(lowStockAnalysis(scene));
        }
        if (hasAny(normalized, "补货", "采购建议", "补仓")) {
            return response(replenishSuggestion(scene));
        }
        if (hasAny(normalized, "异常", "波动", "突增", "突减")) {
            return response(abnormalInventoryHint(scene));
        }
        if (hasAny(normalized, "日报", "日报表", "今日总结")) {
            return response(periodicReport(scene, 1, "日报"));
        }
        if (hasAny(normalized, "周报", "每周", "7天")) {
            return response(periodicReport(scene, 7, "周报"));
        }
        if (hasAny(normalized, "sop", "流程", "作业", "操作规范", "标准流程")) {
            return response(sopAnswer(scene, normalized));
        }
        if (hasAny(normalized, "经营", "利润", "策略", "企业建议", "业务建议")) {
            return response(enterpriseAdvice(scene));
        }

        return response(Map.of(
                "reply", "已识别为通用咨询。当前可提问：库存概览、低库存分析、补货建议、异常波动、日报周报、SOP。",
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.72
        ));
    }

    @SuppressWarnings("unchecked")
    private AiProviderResponse response(Map<String, Object> map) {
        AiProviderResponse.Builder builder = AiProviderResponse.builder(
                        String.valueOf(map.getOrDefault("scene", "")),
                        String.valueOf(map.getOrDefault("provider", providerName())))
                .reply(String.valueOf(map.getOrDefault("reply", "")));
        Object confidence = map.get("confidence");
        if (confidence instanceof Number number) {
            builder.confidence(number);
        }
        Object metrics = map.get("metrics");
        if (metrics instanceof Map<?, ?> rawMetrics) {
            builder.metrics((Map<String, Object>) rawMetrics);
        }
        Object suggestions = map.get("suggestions");
        if (suggestions instanceof List<?> rawSuggestions) {
            builder.suggestions((List<Map<String, Object>>) rawSuggestions);
        }
        return builder.build();
    }

    private Map<String, Object> inventoryOverview(String scene) {
        List<InventoryEntity> rows = inventoryRows();
        Map<Long, ProductEntity> productMap = lowStockPolicyService.buildProductMapFromInventory(rows);
        BigDecimal totalAvailable = rows.stream()
                .map(InventoryEntity::getAvailableQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStockCount = lowStockPolicyService.countLowStock(rows, productMap);
        BigDecimal threshold = lowStockPolicyService.defaultThreshold();

        String reply = String.format(
                "库存概览：共 %d 个库存点，可用库存合计 %s。低库存 %d 个（口径：优先按商品安全库存，缺省阈值 %s）。",
                rows.size(),
                totalAvailable.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                lowStockCount,
                threshold.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.91,
                "metrics", Map.of(
                        "inventoryPoints", rows.size(),
                        "totalAvailable", totalAvailable,
                        "lowStockCount", lowStockCount,
                        "lowStockThreshold", threshold
                )
        );
    }

    private Map<String, Object> lowStockAnalysis(String scene) {
        List<InventoryEntity> rows = inventoryRows();
        Map<Long, ProductEntity> productMap = lowStockPolicyService.buildProductMapFromInventory(rows);
        long lowStockCount = lowStockPolicyService.countLowStock(rows, productMap);
        BigDecimal threshold = lowStockPolicyService.defaultThreshold();
        List<InventoryEntity> lowRows = lowStockPolicyService.filterLowStock(rows, productMap).stream().limit(5).toList();

        String items = lowRows.stream()
                .map(i -> {
                    ProductEntity p = productMap.get(i.getProductId());
                    BigDecimal productThreshold = lowStockPolicyService.thresholdForProduct(p);
                    String productLabel = p == null ? ("商品" + i.getProductId()) : (p.getProductCode() + "/" + p.getProductName());
                    return productLabel + "(可用:" + i.getAvailableQty().stripTrailingZeros().toPlainString()
                            + ",安全库存:" + productThreshold.stripTrailingZeros().toPlainString() + ")";
                })
                .collect(Collectors.joining("；"));

        String reply = lowRows.isEmpty()
                ? "当前未发现低库存 SKU，可继续按日巡检库存阈值。"
                : "低库存分析完成，风险 TOP SKU：" + items + "。建议优先补货并核查在途库存。";

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", lowRows.isEmpty() ? 0.88 : 0.93,
                "metrics", Map.of(
                        "lowStockCount", lowStockCount,
                        "previewCount", lowRows.size(),
                        "threshold", threshold
                )
        );
    }

    private Map<String, Object> replenishSuggestion(String scene) {
        List<InventoryEntity> rows = inventoryRows();
        Map<Long, ProductEntity> productMap = lowStockPolicyService.buildProductMapFromInventory(rows);
        List<InventoryEntity> candidateRows = lowStockPolicyService.filterLowStock(rows, productMap).stream().limit(8).toList();

        List<Map<String, Object>> suggestions = new ArrayList<>();
        int factFailedCount = 0;

        for (InventoryEntity i : candidateRows) {
            ProductEntity p = productMap.get(i.getProductId());
            Map<String, Object> factValidation = lowStockPolicyService.buildSuggestionFactValidation(i, p);
            boolean passed = Boolean.TRUE.equals(factValidation.get("passed"));
            if (!passed) {
                factFailedCount++;
                continue;
            }

            BigDecimal threshold = lowStockPolicyService.thresholdForProduct(p);
            BigDecimal shortage = threshold.subtract(i.getAvailableQty()).max(BigDecimal.ZERO);
            suggestions.add(Map.of(
                    "productId", i.getProductId(),
                    "productCode", p == null ? "" : p.getProductCode(),
                    "productName", p == null ? "" : p.getProductName(),
                    "warehouseId", i.getWarehouseId(),
                    "locationId", i.getLocationId(),
                    "currentQty", i.getAvailableQty(),
                    "safetyStock", threshold,
                    "suggestReplenishQty", shortage,
                    "factValidation", factValidation
            ));

            if (suggestions.size() >= 5) {
                break;
            }
        }

        String reply = suggestions.isEmpty()
                ? "当前库存充足或事实校验未通过，暂无可执行补货建议。"
                : "补货建议已生成：优先处理前 5 个低库存 SKU，且均通过事实引用校验。";

        return Map.of(
                "reply", reply,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.89,
                "suggestions", suggestions,
                "metrics", Map.of(
                        "suggestionCount", suggestions.size(),
                        "factValidationFailedCount", factFailedCount
                )
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
                .map(t -> t.getBizNo() + "/商品" + t.getProductId() + " 变动 " + t.getChangeQty().stripTrailingZeros().toPlainString())
                .collect(Collectors.joining("；"));

        String reply = abnormal.isEmpty()
                ? "近 3 天未发现明显异常库存波动。"
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
                "%s（%s ~ %s）：入库 %s，出库 %s，净变化 %s。建议关注高频出库 SKU 与低库存联动。",
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
            sop = "入库 SOP：到货核对 -> 质检 -> 上架分配库位 -> 入账 -> 抽检复核。";
        } else if (hasAny(normalized, "出库")) {
            sop = "出库 SOP：拣货波次 -> 复核 -> 打包 -> 发运登记 -> 回写出库单。";
        } else if (hasAny(normalized, "盘点")) {
            sop = "盘点 SOP：冻结范围 -> 现场盘点 -> 差异复核 -> 审批调整 -> 盘点归档。";
        } else {
            sop = "通用 SOP 建议：明确责任人、标准步骤、复核节点与异常升级路径。";
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
        Map<Long, ProductEntity> productMap = lowStockPolicyService.buildProductMapFromInventory(rows);
        long lowCount = lowStockPolicyService.countLowStock(rows, productMap);

        BigDecimal ratio = rows.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(lowCount)
                .divide(BigDecimal.valueOf(rows.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<String> advices = new ArrayList<>();
        if (ratio.compareTo(BigDecimal.valueOf(20)) >= 0) {
            advices.add("低库存占比较高，建议先补齐高周转 SKU 安全库存。");
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
                        "inventoryPoints", rows.size(),
                        "thresholdRule", "商品规格中的“安全库存”优先，缺省阈值 10"
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
