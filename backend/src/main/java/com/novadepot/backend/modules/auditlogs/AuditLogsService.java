package com.novadepot.backend.modules.auditlogs;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.AuditLogEntity;
import com.novadepot.backend.repository.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditLogsService {
    private static final DateTimeFormatter STD_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogsService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public Map<String, Object> list(Integer pageNo,
                                    Integer pageSize,
                                    String module,
                                    String action,
                                    String resourceType,
                                    String resourceId,
                                    String bizNo,
                                    Long operatorId,
                                    String operatorKeyword,
                                    Boolean onlyFailed,
                                    String dateFrom,
                                    String dateTo) {
        int safePageNo = Math.max(1, pageNo == null ? 1 : pageNo);
        int safePageSize = Math.max(1, Math.min(100, pageSize == null ? 20 : pageSize));
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<AuditLogEntity> countQw = buildFilter(module, action, resourceType, resourceId, bizNo, operatorId, operatorKeyword, onlyFailed, dateFrom, dateTo);
        Long total = auditLogMapper.selectCount(countQw);

        List<AuditLogEntity> rows = auditLogMapper.selectAuditPage(
                RequestContext.tenantId(),
                clean(module),
                clean(action),
                clean(resourceType),
                clean(resourceId),
                clean(bizNo),
                operatorId,
                clean(operatorKeyword),
                Boolean.TRUE.equals(onlyFailed),
                parseDateTime(dateFrom, false),
                parseDateTime(dateTo, true),
                offset,
                safePageSize
        );
        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        rows.forEach(item -> list.add(toMap(item, false)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total == null ? 0 : total);
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        result.put("dataSource", "MYSQL");
        return result;
    }

    public Map<String, Object> detail(Long id) {
        AuditLogEntity row = auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getTenantId, RequestContext.tenantId())
                .eq(AuditLogEntity::getId, id));
        if (row == null) {
            row = findNearestAuditLogById(id).orElse(null);
        }
        if (row == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Audit log not found");
        }
        return toMap(row, true);
    }

    public void writeExportCsv(OutputStream outputStream,
                               String module,
                               String action,
                               String resourceType,
                               String resourceId,
                               String bizNo,
                               Long operatorId,
                               String operatorKeyword,
                               Boolean onlyFailed,
                               String dateFrom,
                               String dateTo) throws java.io.IOException {
        List<AuditLogEntity> rows = auditLogMapper.selectAuditExport(
                RequestContext.tenantId(),
                clean(module),
                clean(action),
                clean(resourceType),
                clean(resourceId),
                clean(bizNo),
                operatorId,
                clean(operatorKeyword),
                Boolean.TRUE.equals(onlyFailed),
                parseDateTime(dateFrom, false),
                parseDateTime(dateTo, true)
        );
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write("id,module,action,resourceType,resourceId,bizNo,operatorId,operatorName,occurredAt\n");
            for (AuditLogEntity row : rows) {
                writer.write(value(row.getId()) + "," + escape(row.getModule()) + "," + escape(row.getAction()) + ","
                        + escape(row.getResourceType()) + "," + escape(row.getResourceId()) + "," + escape(row.getBizNo()) + ","
                        + value(row.getOperatorId()) + "," + escape(row.getOperatorName()) + "," + escape(String.valueOf(row.getOccurredAt())) + "\n");
            }
            writer.flush();
        }
    }

    private Optional<AuditLogEntity> findNearestAuditLogById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        long delta = 4096L;
        long lower = id > Long.MIN_VALUE + delta ? id - delta : Long.MIN_VALUE;
        long upper = id < Long.MAX_VALUE - delta ? id + delta : Long.MAX_VALUE;
        List<AuditLogEntity> candidates = auditLogMapper.selectNearestCandidates(RequestContext.tenantId(), lower, upper, 20);
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .min(java.util.Comparator.comparingLong(item -> Math.abs(item.getId() - id)));
    }

    private LambdaQueryWrapper<AuditLogEntity> buildFilter(String module,
                                                           String action,
                                                           String resourceType,
                                                           String resourceId,
                                                           String bizNo,
                                                           Long operatorId,
                                                           String operatorKeyword,
                                                           Boolean onlyFailed,
                                                           String dateFrom,
                                                           String dateTo) {
        LambdaQueryWrapper<AuditLogEntity> qw = new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getTenantId, RequestContext.tenantId());
        if (module != null && !module.isBlank()) {
            qw.like(AuditLogEntity::getModule, module.trim());
        }
        if (action != null && !action.isBlank()) {
            qw.like(AuditLogEntity::getAction, action.trim());
        }
        if (resourceType != null && !resourceType.isBlank()) {
            qw.like(AuditLogEntity::getResourceType, resourceType.trim());
        }
        if (resourceId != null && !resourceId.isBlank()) {
            qw.like(AuditLogEntity::getResourceId, resourceId.trim());
        }
        if (bizNo != null && !bizNo.isBlank()) {
            qw.like(AuditLogEntity::getBizNo, bizNo.trim());
        }
        if (operatorId != null) {
            qw.eq(AuditLogEntity::getOperatorId, operatorId);
        } else if (operatorKeyword != null && !operatorKeyword.isBlank()) {
            String keyword = operatorKeyword.trim();
            qw.and(inner -> inner
                    .like(AuditLogEntity::getOperatorName, keyword)
                    .or()
                    .apply("CAST(operator_id AS CHAR) LIKE {0}", "%" + keyword + "%"));
        }
        if (Boolean.TRUE.equals(onlyFailed)) {
            qw.like(AuditLogEntity::getAction, "FAIL");
        }
        LocalDateTime from = parseDateTime(dateFrom, false);
        LocalDateTime to = parseDateTime(dateTo, true);
        if (from != null) {
            qw.ge(AuditLogEntity::getOccurredAt, from);
        }
        if (to != null) {
            qw.le(AuditLogEntity::getOccurredAt, to);
        }
        return qw;
    }

    private LocalDateTime parseDateTime(String input, boolean endOfDay) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(input.trim());
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(input.trim(), STD_DATE_TIME);
        } catch (Exception ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(input.trim());
            return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") ? "\"" + escaped + "\"" : escaped;
    }

    private Map<String, Object> toMap(AuditLogEntity row, boolean includeDetail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row.getId() == null ? null : String.valueOf(row.getId()));
        map.put("module", row.getModule());
        map.put("action", row.getAction());
        map.put("resourceType", row.getResourceType());
        map.put("resourceId", row.getResourceId());
        map.put("bizNo", row.getBizNo());
        map.put("operatorId", row.getOperatorId());
        map.put("operatorName", row.getOperatorName());
        map.put("beforeJson", row.getBeforeJson());
        map.put("afterJson", row.getAfterJson());
        map.put("ip", row.getIp());
        map.put("userAgent", row.getUserAgent());
        map.put("occurredAt", row.getOccurredAt());

        if (includeDetail) {
            Map<String, Object> beforeObj = parseJsonMap(row.getBeforeJson());
            Map<String, Object> afterObj = parseJsonMap(row.getAfterJson());
            map.put("beforeObject", beforeObj);
            map.put("afterObject", afterObj);
            map.put("diff", buildDiff(beforeObj, afterObj));
        }
        return map;
    }

    private Map<String, Object> parseJsonMap(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Map.of("_raw", raw);
        }
    }

    private List<Map<String, Object>> buildDiff(Map<String, Object> beforeObj, Map<String, Object> afterObj) {
        List<Map<String, Object>> diff = new ArrayList<>();
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        keys.addAll(beforeObj.keySet());
        keys.addAll(afterObj.keySet());
        for (String key : keys) {
            Object before = beforeObj.get(key);
            Object after = afterObj.get(key);
            if ((before == null && after == null) || (before != null && before.equals(after))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("field", key);
            row.put("before", before);
            row.put("after", after);
            diff.add(row);
        }
        return diff;
    }
}
