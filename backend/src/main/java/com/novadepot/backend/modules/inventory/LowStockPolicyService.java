package com.novadepot.backend.modules.inventory;

import com.novadepot.backend.common.utils.SafetyStockParser;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.modules.knowledge.KnowledgeService;
import com.novadepot.backend.repository.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LowStockPolicyService {
    private final ProductMapper productMapper;
    private final KnowledgeService knowledgeService;
    private final BigDecimal defaultLowStockThreshold;

    public LowStockPolicyService(ProductMapper productMapper,
                                 KnowledgeService knowledgeService,
                                 @Value("${app.ai.rule.low-stock-threshold:10}") BigDecimal defaultLowStockThreshold) {
        this.productMapper = productMapper;
        this.knowledgeService = knowledgeService;
        this.defaultLowStockThreshold = defaultLowStockThreshold;
    }

    public Map<Long, ProductEntity> buildProductMapFromInventory(List<InventoryEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Set<Long> productIds = rows.stream()
                .map(InventoryEntity::getProductId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        return buildProductMapByIds(productIds);
    }

    public Map<Long, ProductEntity> buildProductMapByIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectBatchIds(productIds).stream()
                .filter(ProductEntity.class::isInstance)
                .map(ProductEntity.class::cast)
                .collect(Collectors.toMap(ProductEntity::getId, item -> item, (a, b) -> a, HashMap::new));
    }

    public BigDecimal thresholdForProduct(ProductEntity product) {
        return SafetyStockParser.parseOrDefault(product == null ? null : product.getSpec(), defaultThreshold());
    }

    public BigDecimal defaultThreshold() {
        return knowledgeService.decimalRule("LOW_STOCK_DEFAULT_THRESHOLD", defaultLowStockThreshold);
    }

    public boolean isLowStock(InventoryEntity item, Map<Long, ProductEntity> productMap) {
        if (item == null || item.getAvailableQty() == null) {
            return false;
        }
        ProductEntity product = productMap == null ? null : productMap.get(item.getProductId());
        BigDecimal threshold = thresholdForProduct(product);
        return item.getAvailableQty().compareTo(threshold) <= 0;
    }

    public long countLowStock(List<InventoryEntity> rows, Map<Long, ProductEntity> productMap) {
        if (rows == null || rows.isEmpty()) {
            return 0L;
        }
        return rows.stream().filter(item -> isLowStock(item, productMap)).count();
    }

    public List<InventoryEntity> filterLowStock(List<InventoryEntity> rows, Map<Long, ProductEntity> productMap) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(item -> isLowStock(item, productMap))
                .sorted(Comparator.comparing(InventoryEntity::getAvailableQty))
                .toList();
    }

    public Map<String, Object> buildSuggestionFactValidation(InventoryEntity row, ProductEntity product) {
        boolean skuExists = product != null && product.getId() != null;
        boolean inventoryTraceable = row != null && row.getProductId() != null && row.getWarehouseId() != null && row.getLocationId() != null;
        boolean passed = skuExists && inventoryTraceable;
        String reason = passed ? "PASS" : (!skuExists ? "SKU_NOT_FOUND" : "INVENTORY_TRACE_MISSING");

        Map<String, Object> result = new HashMap<>();
        result.put("skuExists", skuExists);
        result.put("inventoryTraceable", inventoryTraceable);
        result.put("passed", passed);
        result.put("reason", reason);
        return result;
    }
}
