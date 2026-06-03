package com.novadepot.backend.modules.partners;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.novadepot.backend.common.cache.ReferenceDataCacheService;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.ImportErrorReportEntity;
import com.novadepot.backend.model.entity.PartnerEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.ImportErrorReportMapper;
import com.novadepot.backend.repository.PartnerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class PartnersService {
    private static final String MODULE = "ERP_PARTNER";
    private static final String RESOURCE_TYPE = "PARTNER";

    private final PartnerMapper partnerMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final ImportErrorReportMapper importErrorReportMapper;
    private final ReferenceDataCacheService cacheService;

    public PartnersService(PartnerMapper partnerMapper,
                           AuditLogRecordService auditLogRecordService,
                           ImportErrorReportMapper importErrorReportMapper,
                           ReferenceDataCacheService cacheService) {
        this.partnerMapper = partnerMapper;
        this.auditLogRecordService = auditLogRecordService;
        this.importErrorReportMapper = importErrorReportMapper;
        this.cacheService = cacheService;
    }

    public List<PartnerEntity> list(String keyword, String partnerType) {
        LambdaQueryWrapper<PartnerEntity> wrapper = new LambdaQueryWrapper<PartnerEntity>()
                .eq(PartnerEntity::getTenantId, RequestContext.tenantId());
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(w -> w.like(PartnerEntity::getPartnerName, like)
                    .or()
                    .like(PartnerEntity::getPartnerCode, like));
        }
        if (StringUtils.hasText(partnerType)) {
            wrapper.eq(PartnerEntity::getPartnerType, normalizeType(partnerType));
        }
        String key = cachePrefix() + "list:keyword:" + cleanKey(keyword) + ":type:" + cleanKey(partnerType);
        return cacheService.getList(key, new TypeReference<List<PartnerEntity>>() {
        }, () -> partnerMapper.selectList(wrapper.orderByDesc(PartnerEntity::getId)));
    }

    public PartnerEntity detail(Long id) {
        PartnerEntity entity = partnerMapper.selectOne(new LambdaQueryWrapper<PartnerEntity>()
                .eq(PartnerEntity::getTenantId, RequestContext.tenantId())
                .eq(PartnerEntity::getId, id));
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "往来单位不存在");
        }
        return entity;
    }

    public Map<String, Object> create(PartnerRequest req) {
        validateUniqueCode(req.getPartnerCode(), null);
        PartnerEntity entity = new PartnerEntity();
        entity.setTenantId(RequestContext.tenantId());
        apply(entity, req);
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(RequestContext.userId());
        entity.setUpdatedBy(RequestContext.userId());
        partnerMapper.insert(entity);
        evictCache();
        auditLogRecordService.record(MODULE, "CREATE", RESOURCE_TYPE, String.valueOf(entity.getId()),
                entity.getPartnerCode(), null, snapshot(entity));
        return Map.of("id", String.valueOf(entity.getId()), "partnerCode", entity.getPartnerCode());
    }

    public Map<String, Object> update(Long id, PartnerRequest req) {
        PartnerEntity entity = detail(id);
        validateUniqueCode(req.getPartnerCode(), id);
        String before = snapshot(entity);
        apply(entity, req);
        entity.setUpdatedBy(RequestContext.userId());
        partnerMapper.updateById(entity);
        evictCache();
        auditLogRecordService.record(MODULE, "UPDATE", RESOURCE_TYPE, String.valueOf(entity.getId()),
                entity.getPartnerCode(), before, snapshot(entity));
        return Map.of("id", String.valueOf(entity.getId()), "partnerCode", entity.getPartnerCode());
    }

    public Map<String, Object> setStatus(Long id, String status) {
        PartnerEntity entity = detail(id);
        String before = snapshot(entity);
        entity.setStatus(status);
        entity.setUpdatedBy(RequestContext.userId());
        partnerMapper.updateById(entity);
        evictCache();
        auditLogRecordService.record(MODULE, status.equals("ACTIVE") ? "ENABLE" : "DISABLE", RESOURCE_TYPE,
                String.valueOf(entity.getId()), entity.getPartnerCode(), before, snapshot(entity));
        return Map.of("id", String.valueOf(entity.getId()), "status", entity.getStatus());
    }

    public String importTemplateCsv() {
        return "单位编码,单位名称,单位类型,联系人,电话,地址,状态,备注\n"
                + "P-DEMO-001,示例往来单位,SUPPLIER,张三,13800000000,上海市,ACTIVE,示例数据";
    }

    public String importErrorReport(String reportId) {
        ImportErrorReportEntity report = importErrorReportMapper.selectOne(new LambdaQueryWrapper<ImportErrorReportEntity>()
                .eq(ImportErrorReportEntity::getTenantId, RequestContext.tenantId())
                .eq(ImportErrorReportEntity::getModule, "PARTNER_IMPORT")
                .eq(ImportErrorReportEntity::getReportId, reportId));
        if (report == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Import error report not found");
        }
        return report.getContent();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCsv(String csvContent) {
        String[] lines = normalizeLines(csvContent);
        String header = lines[0].trim();
        if (!header.contains("单位编码") || !header.contains("单位名称") || !header.contains("单位类型")) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV header must contain Chinese partner columns");
        }
        int successRows = 0;
        int skippedRows = 0;
        List<String> errors = new ArrayList<>();
        StringJoiner reportCsv = new StringJoiner("\n").add("行号,字段,错误原因,原始值");
        Map<String, PartnerEntity> partnersByCode = preloadPartnersByCode(lines);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isBlank()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 8) {
                addImportError(errors, reportCsv, i + 1, "整行", "列数量不足", line);
                continue;
            }
            String code = unquote(cols[0]);
            String name = unquote(cols[1]);
            String type = unquote(cols[2]);
            String status = normalizeStatus(unquote(cols[6]));
            if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) {
                addImportError(errors, reportCsv, i + 1, "单位编码/单位名称", "必填字段不能为空", line);
                continue;
            }
            String normalizedType;
            try {
                normalizedType = normalizeType(type);
            } catch (BizException ex) {
                addImportError(errors, reportCsv, i + 1, "单位类型", "仅允许 SUPPLIER / CUSTOMER / BOTH", type);
                continue;
            }
            if (status == null) {
                addImportError(errors, reportCsv, i + 1, "状态", "仅允许 ACTIVE / DISABLED", cols[6]);
                continue;
            }
            PartnerEntity existed = partnersByCode.get(code);
            if (existed != null) {
                skippedRows++;
                addImportError(errors, reportCsv, i + 1, "单位编码", "编码已存在，已跳过", code);
                continue;
            }
            PartnerEntity entity = new PartnerEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setPartnerCode(code);
            entity.setPartnerName(name);
            entity.setPartnerType(normalizedType);
            entity.setContactName(clean(unquote(cols[3])));
            entity.setPhone(clean(unquote(cols[4])));
            entity.setAddress(clean(unquote(cols[5])));
            entity.setStatus(status);
            entity.setRemark(clean(unquote(cols[7])));
            entity.setCreatedBy(RequestContext.userId());
            entity.setUpdatedBy(RequestContext.userId());
            partnerMapper.insert(entity);
            partnersByCode.put(code, entity);
            successRows++;
        }
        String reportId = null;
        if (!errors.isEmpty()) {
            reportId = String.valueOf(System.currentTimeMillis());
            ImportErrorReportEntity report = new ImportErrorReportEntity();
            report.setTenantId(RequestContext.tenantId());
            report.setModule("PARTNER_IMPORT");
            report.setReportId(reportId);
            report.setContent(reportCsv.toString());
            report.setCreatedBy(RequestContext.userId());
            report.setUpdatedBy(RequestContext.userId());
            importErrorReportMapper.insert(report);
        }
        auditLogRecordService.record("IMPORT", "PARTNER_IMPORT", "PARTNER", null, null, null,
                "{\"totalRows\":" + (lines.length - 1) + ",\"successRows\":" + successRows
                        + ",\"failedRows\":" + errors.size() + ",\"skippedRows\":" + skippedRows
                        + ",\"reportId\":\"" + safe(reportId) + "\"}");
        evictCache();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("reportId", reportId);
        summary.put("totalRows", lines.length - 1);
        summary.put("successRows", successRows);
        summary.put("failedRows", errors.size());
        summary.put("skippedRows", skippedRows);
        summary.put("errors", errors);
        return summary;
    }

    private Map<String, PartnerEntity> preloadPartnersByCode(String[] lines) {
        Set<String> codes = java.util.Arrays.stream(lines)
                .skip(1)
                .filter(line -> line != null && !line.trim().isBlank())
                .map(line -> line.split(",", -1))
                .filter(cols -> cols.length >= 1)
                .map(cols -> unquote(cols[0]))
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (codes.isEmpty()) {
            return new HashMap<>();
        }
        return partnerMapper.selectList(new LambdaQueryWrapper<PartnerEntity>()
                        .eq(PartnerEntity::getTenantId, RequestContext.tenantId())
                        .in(PartnerEntity::getPartnerCode, codes))
                .stream()
                .collect(Collectors.toMap(PartnerEntity::getPartnerCode, item -> item, (a, b) -> a, HashMap::new));
    }

    private void apply(PartnerEntity entity, PartnerRequest req) {
        entity.setPartnerCode(req.getPartnerCode().trim());
        entity.setPartnerName(req.getPartnerName().trim());
        entity.setPartnerType(normalizeType(req.getPartnerType()));
        entity.setContactName(clean(req.getContactName()));
        entity.setPhone(clean(req.getPhone()));
        entity.setAddress(clean(req.getAddress()));
        entity.setRemark(clean(req.getRemark()));
    }

    private void validateUniqueCode(String code, Long selfId) {
        PartnerEntity same = partnerMapper.selectOne(new LambdaQueryWrapper<PartnerEntity>()
                .eq(PartnerEntity::getTenantId, RequestContext.tenantId())
                .eq(PartnerEntity::getPartnerCode, code));
        if (same == null || (selfId != null && selfId.equals(same.getId()))) {
            return;
        }
        throw new BizException(ErrorCode.BIZ_ERROR.code(), "partnerCode already exists");
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (List.of("SUPPLIER", "CUSTOMER", "BOTH").contains(value)) {
            return value;
        }
        throw new BizException(ErrorCode.BIZ_ERROR.code(), "partnerType must be SUPPLIER, CUSTOMER, or BOTH");
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeStatus(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "ACTIVE";
        if (List.of("ACTIVE", "DISABLED").contains(normalized)) {
            return normalized;
        }
        return null;
    }

    private String[] normalizeLines(String csvContent) {
        if (!StringUtils.hasText(csvContent)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV content cannot be empty");
        }
        String[] lines = csvContent.replace("\uFEFF", "").replace("\r\n", "\n").replace("\r", "\n").split("\n");
        if (lines.length < 2) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "CSV must contain header and at least one data row");
        }
        return lines;
    }

    private String unquote(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"").trim();
        }
        return trimmed;
    }

    private void addImportError(List<String> errors, StringJoiner reportCsv, int lineNo, String field, String error, String rawLine) {
        errors.add("line " + lineNo + " [" + field + "]: " + error);
        reportCsv.add(lineNo + "," + escape(field) + "," + escape(error) + "," + escape(rawLine));
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String snapshot(PartnerEntity entity) {
        return "{\"partnerCode\":\"" + safe(entity.getPartnerCode()) + "\",\"partnerName\":\"" + safe(entity.getPartnerName())
                + "\",\"partnerType\":\"" + safe(entity.getPartnerType()) + "\",\"status\":\"" + safe(entity.getStatus()) + "\"}";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "'");
    }

    private String cachePrefix() {
        return "novadepot:ref:tenant:" + RequestContext.tenantId() + ":partners:";
    }

    private String cleanKey(String value) {
        return value == null || value.isBlank() ? "all" : value.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private void evictCache() {
        cacheService.evictPrefix(cachePrefix());
    }
}
