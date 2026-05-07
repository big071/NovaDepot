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
        String before = snapshot(existed);
        existed.setProductCode(req.getProductCode());
        existed.setProductName(req.getProductName());
        existed.setCategoryId(req.getCategoryId());
        existed.setUnitId(req.getUnitId());
        existed.setBarcode(req.getBarcode());
        productMapper.updateById(existed);
        auditLogRecordService.record("PRODUCT", "UPDATE", "PRODUCT", String.valueOf(existed.getId()),
                existed.getProductCode(), before, snapshot(existed));
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
        return "商品编码,商品名称,分类编码,单位编码,条码,规格,启用批次,保质期天数,状态\n"
                + "SKU-DEMO-001,示例商品,CAT-FOOD,UNIT-PCS,690000000001,箱规 1*12,否,365,ACTIVE";
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
        CsvImportResult result = parseProductCsv(csvContent);
        String reportId = saveReportIfNeeded("PRODUCT_IMPORT", result.reportCsv, result.errors);
        auditLogRecordService.record("IMPORT", "PRODUCT_IMPORT", "PRODUCT", null, null, null,
                "{\"totalRows\":" + result.totalRows
                        + ",\"successRows\":" + result.successRows
                        + ",\"failedRows\":" + result.errors.size()
                        + ",\"skippedRows\":" + result.skippedRows
                        + ",\"reportId\":\"" + safe(reportId) + "\"}");
        return result.toSummary(reportId);
    }

    private CsvImportResult parseProductCsv(String csvContent) {
        String[] lines = normalizeLines(csvContent);
        String header = lines[0].trim();
        if (!header.contains("商品编码") || !header.contains("商品名称")) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV header must contain Chinese columns: 商品编码, 商品名称");
        }
        CsvImportResult result = new CsvImportResult(lines.length - 1);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isBlank()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 9) {
                result.addError(i + 1, "整行", "列数量不足", line);
                continue;
            }
            String code = unquote(cols[0]);
            String name = unquote(cols[1]);
            Long categoryId = parseCategoryId(unquote(cols[2]));
            Long unitId = parseUnitId(unquote(cols[3]));
            String barcode = unquote(cols[4]);
            String spec = unquote(cols[5]);
            Integer batchEnabled = parseBooleanFlag(unquote(cols[6]));
            Integer shelfLifeDays = parseInteger(unquote(cols[7]));
            String status = normalizeStatus(unquote(cols[8]));
            if (code.isBlank() || name.isBlank()) {
                result.addError(i + 1, "商品编码/商品名称", "必填字段不能为空", line);
                continue;
            }
            if (categoryId == null || unitId == null) {
                result.addError(i + 1, "分类编码/单位编码", "分类或单位不存在", line);
                continue;
            }
            if (batchEnabled == null || shelfLifeDays == null || shelfLifeDays < 0 || status == null) {
                result.addError(i + 1, "启用批次/保质期天数/状态", "字段格式非法", line);
                continue;
            }
            ProductEntity existed = detailByCode(code);
            if (existed != null) {
                result.skippedRows++;
                result.addError(i + 1, "商品编码", "SKU 已存在，已跳过", code);
                continue;
            }
            ProductEntity entity = new ProductEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setProductCode(code);
            entity.setProductName(name);
            entity.setCategoryId(categoryId);
            entity.setUnitId(unitId);
            entity.setBarcode(barcode);
            entity.setSpec(spec);
            entity.setBatchEnabled(batchEnabled);
            entity.setShelfLifeDays(shelfLifeDays);
            entity.setStatus(status);
            entity.setCreatedBy(RequestContext.userId());
            entity.setUpdatedBy(RequestContext.userId());
            productMapper.insert(entity);
            result.successRows++;
        }
        return result;
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

    private String saveReportIfNeeded(String module, StringJoiner reportCsv, List<String> errors) {
        if (errors.isEmpty()) return null;
        String reportId = String.valueOf(System.currentTimeMillis());
        ImportErrorReportEntity report = new ImportErrorReportEntity();
        report.setTenantId(RequestContext.tenantId());
        report.setModule(module);
        report.setReportId(reportId);
        report.setContent(reportCsv.toString());
        report.setCreatedBy(RequestContext.userId());
        report.setUpdatedBy(RequestContext.userId());
        importErrorReportMapper.insert(report);
        return reportId;
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
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseCategoryId(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().matches("\\d+")) return parseLong(value);
        return switch (value.trim()) {
            case "CAT-FOOD" -> 9001L;
            case "CAT-HOUSE" -> 9002L;
            default -> null;
        };
    }

    private Long parseUnitId(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().matches("\\d+")) return parseLong(value);
        return switch (value.trim()) {
            case "UNIT-PCS" -> 9101L;
            case "UNIT-BOX" -> 9102L;
            default -> null;
        };
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value == null || value.isBlank() ? "0" : value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseBooleanFlag(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.equals("是") || normalized.equalsIgnoreCase("true") || normalized.equals("1")) return 1;
        if (normalized.equals("否") || normalized.equalsIgnoreCase("false") || normalized.equals("0") || normalized.isBlank()) return 0;
        return null;
    }

    private String normalizeStatus(String value) {
        String normalized = value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("ACTIVE") || normalized.equals("DISABLED")) return normalized;
        return null;
    }

    private void validateUniqueCode(String productCode, Long selfId) {
        if (productCode == null || productCode.isBlank()) return;
        ProductEntity sameCode = detailByCode(productCode);
        if (sameCode == null || (selfId != null && selfId.equals(sameCode.getId()))) return;
        throw new BizException(ErrorCode.BIZ_ERROR.code(), "productCode already exists");
    }

    private String snapshot(ProductEntity entity) {
        return "{\"productName\":\"" + safe(entity.getProductName()) + "\",\"barcode\":\"" + safe(entity.getBarcode()) + "\"}";
    }

    private class CsvImportResult {
        private final int totalRows;
        private int successRows;
        private int skippedRows;
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
            summary.put("skippedRows", skippedRows);
            summary.put("errors", errors);
            return summary;
        }
    }
}
