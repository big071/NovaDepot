package com.novadepot.backend.modules.auditlogs;

import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AuditLogEntity;
import com.novadepot.backend.repository.AuditLogMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogRecordService {
    private final AuditLogMapper auditLogMapper;

    public AuditLogRecordService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(String module,
                       String action,
                       String resourceType,
                       String resourceId,
                       String bizNo,
                       String beforeJson,
                       String afterJson) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setModule(module);
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setBizNo(bizNo);
        entity.setOperatorId(RequestContext.userId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            entity.setOperatorName(auth.getName());
        }
        entity.setBeforeJson(beforeJson);
        entity.setAfterJson(afterJson);
        entity.setOccurredAt(LocalDateTime.now());
        entity.setCreatedBy(RequestContext.userId());
        entity.setUpdatedBy(RequestContext.userId());
        auditLogMapper.insert(entity);
    }
}
