package com.novadepot.backend.modules.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.ImportErrorReportEntity;
import com.novadepot.backend.model.entity.ProductEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.ImportErrorReportMapper;
import com.novadepot.backend.repository.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

@Service
public class ProductsService {
    private final ProductMapper productMapper;
    private final ImportErrorReportMapper importErrorReportMapper;
    private final AuditLogRecordService auditLogRecordService;

    public ProductsService(ProductMapper productMapper,
                           ImportErrorReportMapper importErrorReportMapper,
                           AuditLogRecordService auditLogRecordService) {
        this.productMapper = productMapper;
        this.importErrorReportMapper = importErrorReportMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<ProductEntity> list() {
        return productMapper.selectList(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(ProductEntity::getId));
    }

    public ProductEntity detail(Long id) {
        return productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .eq(ProductEntity::getId, id));
    }

    public ProductEntity detailByCode(String productCode) {
        return productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .eq(ProductEntity::getProductCode, productCode));
    }

    public Map<String, Object> create(ProductCreateRequest req) {
        validateUniqueCode(req.getProductCode(), null);
        ProductEntity entity = new ProductEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setProductCode(req.getProductCode());
        entity.setProductName(req.getProductName());
        entity.setCategoryId(req.getCategoryId());
        entity.setUnitId(req.getUnitId());
        entity.setBarcode(req.getBarcode());
        entity.setStatus("ACTIVE");
        entity.setBatchEnabled(0);
        productMapper.insert(entity);
        auditLogRecordService.record("PRODUCT", "CREATE", "PRODUCT", String.valueOf(entity.getId()),
                entity.getProductCode(), null, "{\"status\":\"ACTIVE\"}");
        return Map.of("id", entity.getId(), "productCode", entity.getProductCode());
    }

    public Map<String, Object> update(Long id, ProductCreateRequest req) {
        ProductEntity existed = detail(id);
        if (existed == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Product not found");
        }
        validateUniqueCode(req.getProductCode(), existed.getId());
        String before = "{\"productName\":\"" + safe(existed.getProductName()) + "\",\"barcode\":\"" + safe(existed.getBarcode()) + "\"}";
        existed.setProductCode(req.getProductCode());
        existed.setProductName(req.getProductName());
        existed.setCategoryId(req.getCategoryId());
        existed.setUnitId(req.getUnitId());
        existed.setBarcode(req.getBarcode());
        productMapper.updateById(existed);
        String after = "{\"productName\":\"" + safe(existed.getProductName()) + "\",\"barcode\":\"" + safe(existed.getBarcode()) + "\"}";
        auditLogRecordService.record("PRODUCT", "UPDATE", "PRODUCT", String.valueOf(existed.getId()),
                existed.getProductCode(), before, after);
        return Map.of("id", existed.getId(), "productCode", existed.getProductCode());
    }

    public String exportCsv() {
        List<ProductEntity> products = list();
        StringJoiner csv = new StringJoiner("\n");
        csv.add("productCode,productName,categoryId,unitId,barcode,status");
        for (ProductEntity item : products) {
            csv.add(escape(item.getProductCode()) + ","
                    + escape(item.getProductName()) + ","
                    + value(item.getCategoryId()) + ","
                    + value(item.getUnitId()) + ","
                    + escape(item.getBarcode()) + ","
                    + escape(item.getStatus()));
        }
        auditLogRecordService.record("PRODUCT", "EXPORT", "PRODUCT", null, null, null,
                "{\"count\":" + products.size() + "}");
        return csv.toString();
    }

    public String importTemplateCsv() {
        auditLogRecordService.record("PRODUCT", "IMPORT_TEMPLATE", "PRODUCT", null, null, null,
                "{\"fields\":\"productCode,productName,categoryId,unitId,barcode\"}");
        return "productCode,productName,categoryId,unitId,barcode\n"
                + "SKU-1001,示例商品,9001,9101,690300010001";
    }

    public String importErrorReport(String reportId) {
        ImportErrorReportEntity report = importErrorReportMapper.selectOne(new LambdaQueryWrapper<ImportErrorReportEntity>()
                .eq(ImportErrorReportEntity::getTenantId, RequestContext.tenantId())
                .eq(ImportErrorReportEntity::getModule, "PRODUCT_IMPORT")
                .eq(ImportErrorReportEntity::getReportId, reportId));
        if (report == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Import error report not found");
        }
        return report.getContent();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCsv(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV content cannot be empty");
        }

        String normalized = csvContent.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        if (lines.length < 2) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV must contain header and at least one data row");
        }

        String header = lines[0].trim().toLowerCase(Locale.ROOT);
        if (!header.contains("productcode") || !header.contains("productname")) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV header must contain productCode and productName");
        }

        int totalRows = lines.length - 1;
        int inserted = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        StringJoiner reportCsv = new StringJoiner("\n");
        reportCsv.add("lineNo,error,rawLine");

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isBlank()) {
                continue;
            }
            String[] cols = line.split(",", -1);
            if (cols.length < 5) {
                addImportError(errors, reportCsv, i + 1, "insufficient columns", line);
                continue;
            }

            String productCode = unquote(cols[0]);
            String productName = unquote(cols[1]);
            Long categoryId = parseLong(unquote(cols[2]));
            Long unitId = parseLong(unquote(cols[3]));
            String barcode = unquote(cols[4]);

            if (productCode.isBlank() || productName.isBlank()) {
                addImportError(errors, reportCsv, i + 1, "productCode/productName cannot be empty", line);
                continue;
            }
            if (categoryId == null || unitId == null) {
                addImportError(errors, reportCsv, i + 1, "categoryId/unitId invalid", line);
                continue;
            }

            ProductEntity existed = productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                    .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                    .eq(ProductEntity::getProductCode, productCode));

            if (existed == null) {
                ProductEntity entity = new ProductEntity();
                entity.setTenantId(RequestContext.tenantId());
                entity.setProductCode(productCode);
                entity.setProductName(productName);
                entity.setCategoryId(categoryId);
                entity.setUnitId(unitId);
                entity.setBarcode(barcode);
                entity.setStatus("ACTIVE");
                entity.setBatchEnabled(0);
                productMapper.insert(entity);
                inserted++;
            } else {
                existed.setProductName(productName);
                existed.setCategoryId(categoryId);
                existed.setUnitId(unitId);
                existed.setBarcode(barcode);
                if (Objects.isNull(existed.getStatus()) || existed.getStatus().isBlank()) {
                    existed.setStatus("ACTIVE");
                }
                productMapper.updateById(existed);
                updated++;
            }
        }

        String reportId = null;
        if (!errors.isEmpty()) {
            reportId = String.valueOf(System.currentTimeMillis());
            ImportErrorReportEntity report = new ImportErrorReportEntity();
            report.setTenantId(RequestContext.tenantId());
            report.setModule("PRODUCT_IMPORT");
            report.setReportId(reportId);
            report.setContent(reportCsv.toString());
            report.setCreatedBy(RequestContext.userId());
            report.setUpdatedBy(RequestContext.userId());
            importErrorReportMapper.insert(report);
        }

        auditLogRecordService.record("PRODUCT", "IMPORT", "PRODUCT", null, null, null,
                "{\"totalRows\":" + totalRows
                        + ",\"inserted\":" + inserted
                        + ",\"updated\":" + updated
                        + ",\"errorCount\":" + errors.size()
                        + ",\"reportId\":\"" + safe(reportId) + "\"}");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRows", totalRows);
        summary.put("inserted", inserted);
        summary.put("updated", updated);
        summary.put("successCount", inserted + updated);
        summary.put("errorCount", errors.size());
        summary.put("reportId", reportId);
        summary.put("errors", errors);
        return summary;
    }

    private void addImportError(List<String> errors, StringJoiner reportCsv, int lineNo, String error, String rawLine) {
        String message = "line " + lineNo + ": " + error;
        errors.add(message);
        reportCsv.add(lineNo + "," + escape(error) + "," + escape(rawLine));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private String value(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private String unquote(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"").trim();
        }
        return trimmed;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void validateUniqueCode(String productCode, Long selfId) {
        if (productCode == null || productCode.isBlank()) {
            return;
        }
        ProductEntity sameCode = productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getTenantId, RequestContext.tenantId())
                .eq(ProductEntity::getProductCode, productCode));
        if (sameCode == null) {
            return;
        }
        if (selfId != null && selfId.equals(sameCode.getId())) {
            return;
        }
        throw new BizException(ErrorCode.BIZ_ERROR.code(), "productCode already exists");
    }
}
