package com.novadepot.backend.modules.auditlogs;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AuditLogEntity;
import com.novadepot.backend.repository.AuditLogMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditLogCleanupService {
    private final AuditLogMapper auditLogMapper;
    private final AuditLogRecordService auditLogRecordService;

    @Value("${app.audit.retention-days:90}")
    private int retentionDays;

    public AuditLogCleanupService(AuditLogMapper auditLogMapper, AuditLogRecordService auditLogRecordService) {
        this.auditLogMapper = auditLogMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public Map<String, Object> cleanup() {
        RequestContext.setTenantId(1L);
        RequestContext.setUserId(1L);
        int safeDays = Math.max(1, retentionDays);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(safeDays);
        Long deleted = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getTenantId, RequestContext.tenantId())
                .lt(AuditLogEntity::getOccurredAt, cutoff));
        auditLogMapper.delete(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getTenantId, RequestContext.tenantId())
                .lt(AuditLogEntity::getOccurredAt, cutoff));
        auditLogRecordService.record("SYSTEM", "AUDIT_LOG_CLEANUP", "AUDIT_LOG", "RETENTION", null, null,
                "{\"retentionDays\":" + safeDays + ",\"cutoff\":\"" + cutoff + "\",\"deleted\":" + (deleted == null ? 0 : deleted) + "}");
        return Map.of("retentionDays", safeDays, "cutoff", cutoff.toString(), "deleted", deleted == null ? 0 : deleted);
    }
}
