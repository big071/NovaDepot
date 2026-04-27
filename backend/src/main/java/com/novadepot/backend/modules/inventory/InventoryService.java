package com.novadepot.backend.modules.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class InventoryService {
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final LowStockPolicyService lowStockPolicyService;
    private final AuditLogRecordService auditLogRecordService;

    public InventoryService(InventoryMapper inventoryMapper,
                            InventoryTransactionMapper inventoryTransactionMapper,
                            LowStockPolicyService lowStockPolicyService,
                            AuditLogRecordService auditLogRecordService) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.lowStockPolicyService = lowStockPolicyService;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<InventoryEntity> list() {
        return inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(InventoryEntity::getId));
    }

    public List<InventoryTransactionEntity> transactions() {
        return inventoryTransactionMapper.selectList(new LambdaQueryWrapper<InventoryTransactionEntity>()
                .eq(InventoryTransactionEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(InventoryTransactionEntity::getOccurredAt)
                .last("limit 200"));
    }

    public List<InventoryEntity> lowStockAlerts() {
        List<InventoryEntity> rows = list();
        return lowStockPolicyService.filterLowStock(rows, lowStockPolicyService.buildProductMapFromInventory(rows));
    }

    public String exportCsv() {
        List<InventoryEntity> rows = list();
        StringJoiner csv = new StringJoiner("\n");
        csv.add("warehouseId,locationId,productId,availableQty,lockedQty,inTransitQty");
        for (InventoryEntity item : rows) {
            csv.add(item.getWarehouseId() + ","
                    + item.getLocationId() + ","
                    + item.getProductId() + ","
                    + item.getAvailableQty() + ","
                    + item.getLockedQty() + ","
                    + item.getInTransitQty());
        }
        auditLogRecordService.record("INVENTORY", "EXPORT", "INVENTORY", null, null, null,
                "{\"count\":" + rows.size() + "}");
        return csv.toString();
    }

    public List<String> exportFieldDescriptions() {
        return List.of(
                "warehouseId: 仓库ID",
                "locationId: 库位ID",
                "productId: 商品ID",
                "availableQty: 可用库存",
                "lockedQty: 锁定库存",
                "inTransitQty: 在途库存",
                "lowStockRule: 可用库存 <= 商品安全库存（spec 中的“安全库存=XX”，缺省阈值 10）"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCsv(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV content cannot be empty");
        }
        String normalized = csvContent.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        if (lines.length < 2) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV must contain header and at least one row");
        }

        int totalRows = lines.length - 1;
        int inserted = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isBlank()) {
                continue;
            }
            String[] cols = line.split(",", -1);
            if (cols.length < 6) {
                errors.add("line " + (i + 1) + ": insufficient columns");
                continue;
            }

            Long warehouseId = parseLong(cols[0]);
            Long locationId = parseLong(cols[1]);
            Long productId = parseLong(cols[2]);
            BigDecimal availableQty = parseDecimal(cols[3]);
            BigDecimal lockedQty = parseDecimal(cols[4]);
            BigDecimal inTransitQty = parseDecimal(cols[5]);
            if (warehouseId == null || locationId == null || productId == null
                    || availableQty == null || lockedQty == null || inTransitQty == null) {
                errors.add("line " + (i + 1) + ": invalid value");
                continue;
            }

            InventoryEntity existed = inventoryMapper.selectOne(new LambdaQueryWrapper<InventoryEntity>()
                    .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                    .eq(InventoryEntity::getWarehouseId, warehouseId)
                    .eq(InventoryEntity::getLocationId, locationId)
                    .eq(InventoryEntity::getProductId, productId));
            if (existed == null) {
                InventoryEntity row = new InventoryEntity();
                row.setTenantId(RequestContext.tenantId());
                row.setWarehouseId(warehouseId);
                row.setLocationId(locationId);
                row.setProductId(productId);
                row.setAvailableQty(availableQty);
                row.setLockedQty(lockedQty);
                row.setInTransitQty(inTransitQty);
                row.setVersionNo(0);
                inventoryMapper.insert(row);
                inserted++;
            } else {
                existed.setAvailableQty(availableQty);
                existed.setLockedQty(lockedQty);
                existed.setInTransitQty(inTransitQty);
                inventoryMapper.updateById(existed);
                updated++;
            }
        }

        auditLogRecordService.record("INVENTORY", "IMPORT", "INVENTORY", null, null, null,
                "{\"totalRows\":" + totalRows
                        + ",\"inserted\":" + inserted
                        + ",\"updated\":" + updated
                        + ",\"errorCount\":" + errors.size() + "}");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRows", totalRows);
        summary.put("inserted", inserted);
        summary.put("updated", updated);
        summary.put("successCount", inserted + updated);
        summary.put("errorCount", errors.size());
        summary.put("errors", errors);
        return summary;
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
