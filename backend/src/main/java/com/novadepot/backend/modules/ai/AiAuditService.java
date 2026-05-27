package com.novadepot.backend.modules.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AuditLogEntity;
import com.novadepot.backend.repository.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiAuditService {
    private static final Logger log = LoggerFactory.getLogger(AiAuditService.class);

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AiAuditService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void writeAudit(String action, Long resourceId, String bizNo, Object before, Object after) {
        try {
            AuditLogEntity audit = new AuditLogEntity();
            audit.setTenantId(RequestContext.tenantId());
            audit.setModule("AI");
            audit.setAction(action);
            audit.setResourceType("AI_CONVERSATION");
            audit.setResourceId(resourceId == null ? null : String.valueOf(resourceId));
            audit.setBizNo(bizNo);
            audit.setOperatorId(RequestContext.userId());
            audit.setBeforeJson(before == null ? null : objectMapper.writeValueAsString(before));
            audit.setAfterJson(after == null ? null : objectMapper.writeValueAsString(after));
            audit.setOccurredAt(LocalDateTime.now());
            audit.setCreatedBy(RequestContext.userId());
            audit.setUpdatedBy(RequestContext.userId());
            auditLogMapper.insert(audit);
        } catch (Exception ex) {
            log.warn("Failed to write AI audit log: {}", ex.getMessage());
        }
    }
}
