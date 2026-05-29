package com.novadepot.backend.modules.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.ImportErrorReportEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.model.entity.WarehouseEntity;
import com.novadepot.backend.model.entity.WarehouseLocationEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.ImportErrorReportMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.ProductMapper;
import com.novadepot.backend.repository.WarehouseLocationMapper;
import com.novadepot.backend.repository.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final LowStockPolicyService lowStockPolicyService;
    private final AuditLogRecordService auditLogRecordService;
    private final ImportErrorReportMapper importErrorReportMapper;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseLocationMapper locationMapper;
    private final ProductMapper productMapper;

    public InventoryService(InventoryMapper inventoryMapper,
                            InventoryTransactionMapper inventoryTransactionMapper,
                            LowStockPolicyService lowStockPolicyService,
                            AuditLogRecordService auditLogRecordService,
                            ImportErrorReportMapper importErrorReportMapper,
                            WarehouseMapper warehouseMapper,
                            WarehouseLocationMapper locationMapper,
                            ProductMapper productMapper) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.lowStockPolicyService = lowStockPolicyService;
        this.auditLogRecordService = auditLogRecordService;
        this.importErrorReportMapper = importErrorReportMapper;
        this.warehouseMapper = warehouseMapper;
        this.locationMapper = locationMapper;
        this.productMapper = productMapper;
    }

    public List<InventoryEntity> list() {
        return inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(InventoryEntity::getId));
    }

    public List<InventoryTransactionEntity> transactions() {
        return inventoryTransactionMapper.selectRecent(RequestContext.tenantId(), 200);
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
            csv.add(item.getWarehouseId() + "," + item.getLocationId() + "," + item.getProductId() + ","
                    + item.getAvailableQty() + "," + item.getLockedQty() + "," + item.getInTransitQty());
        }
        auditLogRecordService.record("INVENTORY", "EXPORT", "INVENTORY", null, null, null,
                "{\"count\":" + rows.size() + "}");
        return csv.toString();
    }

    public String importTemplateCsv() {
        return "仓库编码,库位编码,商品编码,可用数量,批次号,备注\n"
                + "WH-SH-01,A-01-01,SKU-DEMO-001,10,BATCH-202605,示例库存导入";
    }

    public List<String> exportFieldDescriptions() {
        return List.of(
                "仓库编码: 已存在仓库编码",
                "库位编码: 已存在库位编码",
                "商品编码: 已存在商品编码",
                "可用数量: 非负数字，不带千分位",
                "批次号: 可选",
                "备注: 可选"
        );
    }

    public String importErrorReport(String reportId) {
        ImportErrorReportEntity report = importErrorReportMapper.selectOne(new LambdaQueryWrapper<ImportErrorReportEntity>()
                .eq(ImportErrorReportEntity::getTenantId, RequestContext.tenantId())
                .eq(ImportErrorReportEntity::getModule, "INVENTORY_IMPORT")
                .eq(ImportErrorReportEntity::getReportId, reportId));
        if (report == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Import error report not found");
        }
        return report.getContent();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCsv(String csvContent) {
        CsvImportResult result = parseInventoryCsv(csvContent);
        String reportId = saveReportIfNeeded(result.reportCsv, result.errors);
        auditLogRecordService.record("IMPORT", "INVENTORY_IMPORT", "INVENTORY", null, null, null,
                "{\"totalRows\":" + result.totalRows
                        + ",\"successRows\":" + result.successRows
                        + ",\"failedRows\":" + result.errors.size()
                        + ",\"reportId\":\"" + safe(reportId) + "\"}");
        return result.toSummary(reportId);
    }

    private CsvImportResult parseInventoryCsv(String csvContent) {
        String[] lines = normalizeLines(csvContent);
        String header = lines[0].trim();
        if (!header.contains("仓库编码") || !header.contains("库位编码") || !header.contains("商品编码")) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV header must contain Chinese inventory columns");
        }
        CsvImportResult result = new CsvImportResult(lines.length - 1);
        InventoryImportContext importContext = preloadInventoryContext(lines);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isBlank()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 4) {
                result.addError(i + 1, "整行", "列数量不足", line);
                continue;
            }
            WarehouseEntity warehouse = importContext.warehousesByCode().get(unquote(cols[0]));
            WarehouseLocationEntity location = warehouse == null ? null : importContext.locationsByKey().get(locationKey(warehouse.getId(), unquote(cols[1])));
            ProductEntity product = importContext.productsByCode().get(unquote(cols[2]));
            BigDecimal availableQty = parseDecimal(unquote(cols[3]));
            if (warehouse == null) {
                result.addError(i + 1, "仓库编码", "仓库不存在", cols[0]);
                continue;
            }
            if (location == null || !warehouse.getId().equals(location.getWarehouseId())) {
                result.addError(i + 1, "库位编码", "库位不存在或不属于仓库", cols[1]);
                continue;
            }
            if (product == null) {
                result.addError(i + 1, "商品编码", "商品不存在", cols[2]);
                continue;
            }
            if (availableQty == null || availableQty.compareTo(BigDecimal.ZERO) < 0) {
                result.addError(i + 1, "可用数量", "必须为非负数字", cols[3]);
                continue;
            }
            InventoryKey inventoryKey = new InventoryKey(warehouse.getId(), location.getId(), product.getId());
            InventoryEntity existed = importContext.inventoryByKey().get(inventoryKey);
            BigDecimal before = existed == null ? BigDecimal.ZERO : existed.getAvailableQty();
            if (existed == null) {
                existed = new InventoryEntity();
                existed.setTenantId(RequestContext.tenantId());
                existed.setWarehouseId(warehouse.getId());
                existed.setLocationId(location.getId());
                existed.setProductId(product.getId());
                existed.setLockedQty(BigDecimal.ZERO);
                existed.setInTransitQty(BigDecimal.ZERO);
                existed.setVersionNo(0);
                existed.setCreatedBy(RequestContext.userId());
            }
            existed.setAvailableQty(availableQty);
            existed.setUpdatedBy(RequestContext.userId());
            if (existed.getId() == null) {
                inventoryMapper.insert(existed);
            } else {
                inventoryMapper.updateById(existed);
            }
            importContext.inventoryByKey().put(inventoryKey, existed);
            writeImportTransaction(warehouse.getId(), location.getId(), product.getId(), before, availableQty);
            result.successRows++;
        }
        return result;
    }

    private InventoryImportContext preloadInventoryContext(String[] lines) {
        Set<String> warehouseCodes = new java.util.LinkedHashSet<>();
        Set<String> locationCodes = new java.util.LinkedHashSet<>();
        Set<String> productCodes = new java.util.LinkedHashSet<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isBlank()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 4) continue;
            String warehouseCode = unquote(cols[0]);
            String locationCode = unquote(cols[1]);
            String productCode = unquote(cols[2]);
            if (!warehouseCode.isBlank()) warehouseCodes.add(warehouseCode);
            if (!locationCode.isBlank()) locationCodes.add(locationCode);
            if (!productCode.isBlank()) productCodes.add(productCode);
        }

        Map<String, WarehouseEntity> warehousesByCode = warehouseCodes.isEmpty()
                ? new HashMap<>()
                : warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseEntity>()
                        .eq(WarehouseEntity::getTenantId, RequestContext.tenantId())
                        .in(WarehouseEntity::getWarehouseCode, warehouseCodes))
                .stream()
                .collect(Collectors.toMap(WarehouseEntity::getWarehouseCode, item -> item, (a, b) -> a, HashMap::new));
        Map<String, ProductEntity> productsByCode = productCodes.isEmpty()
                ? new HashMap<>()
                : productMapper.selectList(new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                        .in(ProductEntity::getProductCode, productCodes))
                .stream()
                .collect(Collectors.toMap(ProductEntity::getProductCode, item -> item, (a, b) -> a, HashMap::new));
        Map<String, WarehouseLocationEntity> locationsByKey = locationCodes.isEmpty()
                ? new HashMap<>()
                : locationMapper.selectList(new LambdaQueryWrapper<WarehouseLocationEntity>()
                        .eq(WarehouseLocationEntity::getTenantId, RequestContext.tenantId())
                        .in(WarehouseLocationEntity::getLocationCode, locationCodes))
                .stream()
                .collect(Collectors.toMap(item -> locationKey(item.getWarehouseId(), item.getLocationCode()), item -> item, (a, b) -> a, HashMap::new));

        Set<InventoryKey> inventoryKeys = new java.util.LinkedHashSet<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isBlank()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 4) continue;
            WarehouseEntity warehouse = warehousesByCode.get(unquote(cols[0]));
            ProductEntity product = productsByCode.get(unquote(cols[2]));
            WarehouseLocationEntity location = warehouse == null ? null : locationsByKey.get(locationKey(warehouse.getId(), unquote(cols[1])));
            if (warehouse != null && location != null && product != null) {
                inventoryKeys.add(new InventoryKey(warehouse.getId(), location.getId(), product.getId()));
            }
        }
        Map<InventoryKey, InventoryEntity> inventoryByKey = preloadInventoryByKey(inventoryKeys);
        return new InventoryImportContext(warehousesByCode, locationsByKey, productsByCode, inventoryByKey);
    }

    private Map<InventoryKey, InventoryEntity> preloadInventoryByKey(Set<InventoryKey> inventoryKeys) {
        if (inventoryKeys.isEmpty()) {
            return new HashMap<>();
        }
        Set<Long> warehouseIds = inventoryKeys.stream().map(InventoryKey::warehouseId).collect(Collectors.toSet());
        Set<Long> locationIds = inventoryKeys.stream().map(InventoryKey::locationId).collect(Collectors.toSet());
        Set<Long> productIds = inventoryKeys.stream().map(InventoryKey::productId).collect(Collectors.toSet());
        return inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                        .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                        .in(InventoryEntity::getWarehouseId, warehouseIds)
                        .in(InventoryEntity::getLocationId, locationIds)
                        .in(InventoryEntity::getProductId, productIds))
                .stream()
                .filter(item -> inventoryKeys.contains(new InventoryKey(item.getWarehouseId(), item.getLocationId(), item.getProductId())))
                .collect(Collectors.toMap(item -> new InventoryKey(item.getWarehouseId(), item.getLocationId(), item.getProductId()), item -> item, (a, b) -> a, HashMap::new));
    }

    private String locationKey(Long warehouseId, String locationCode) {
        return warehouseId + "|" + locationCode;
    }

    private void writeImportTransaction(Long warehouseId, Long locationId, Long productId, BigDecimal before, BigDecimal after) {
        InventoryTransactionEntity txn = new InventoryTransactionEntity();
        txn.setTenantId(RequestContext.tenantId());
        txn.setTxnNo("TXN-IMP-" + System.currentTimeMillis() + "-" + productId);
        txn.setBizType("INVENTORY_IMPORT");
        txn.setBizNo("CSV-IMPORT");
        txn.setWarehouseId(warehouseId);
        txn.setLocationId(locationId);
        txn.setProductId(productId);
        txn.setBeforeQty(before);
        txn.setAfterQty(after);
        txn.setChangeQty(after.subtract(before));
        txn.setRequestId("CSV-IMPORT-" + System.nanoTime());
        txn.setOperatorId(RequestContext.userId());
        txn.setOccurredAt(LocalDateTime.now());
        txn.setCreatedBy(RequestContext.userId());
        txn.setUpdatedBy(RequestContext.userId());
        inventoryTransactionMapper.insert(txn);
    }

    private String[] normalizeLines(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV content cannot be empty");
        }
        String[] lines = csvContent.replace("\uFEFF", "").replace("\r\n", "\n").replace("\r", "\n").split("\n");
        if (lines.length < 2) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV must contain header and at least one data row");
        }
        return lines;
    }

    private String saveReportIfNeeded(StringJoiner reportCsv, List<String> errors) {
        if (errors.isEmpty()) return null;
        String reportId = String.valueOf(System.currentTimeMillis());
        ImportErrorReportEntity report = new ImportErrorReportEntity();
        report.setTenantId(RequestContext.tenantId());
        report.setModule("INVENTORY_IMPORT");
        report.setReportId(reportId);
        report.setContent(reportCsv.toString());
        report.setCreatedBy(RequestContext.userId());
        report.setUpdatedBy(RequestContext.userId());
        importErrorReportMapper.insert(report);
        return reportId;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank() || value.contains(",")) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String unquote(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"").trim();
        }
        return trimmed;
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private record InventoryImportContext(Map<String, WarehouseEntity> warehousesByCode,
                                          Map<String, WarehouseLocationEntity> locationsByKey,
                                          Map<String, ProductEntity> productsByCode,
                                          Map<InventoryKey, InventoryEntity> inventoryByKey) {
    }

    private record InventoryKey(Long warehouseId, Long locationId, Long productId) {
    }

    private class CsvImportResult {
        private final int totalRows;
        private int successRows;
        private final List<String> errors = new ArrayList<>();
        private final StringJoiner reportCsv = new StringJoiner("\n").add("行号,字段,错误原因,原始值");

        private CsvImportResult(int totalRows) {
            this.totalRows = totalRows;
        }

        private void addError(int lineNo, String field, String error, String rawLine) {
            errors.add("line " + lineNo + " [" + field + "]: " + error);
            reportCsv.add(lineNo + "," + escape(field) + "," + escape(error) + "," + escape(rawLine));
        }

        private Map<String, Object> toSummary(String reportId) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("reportId", reportId);
            summary.put("totalRows", totalRows);
            summary.put("successRows", successRows);
            summary.put("failedRows", errors.size());
            summary.put("skippedRows", 0);
            summary.put("errors", errors);
            return summary;
        }
    }
}
