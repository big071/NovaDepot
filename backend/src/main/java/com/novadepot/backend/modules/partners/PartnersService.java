package com.novadepot.backend.modules.partners;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.PartnerEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.PartnerMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PartnersService {
    private static final String MODULE = "ERP_PARTNER";
    private static final String RESOURCE_TYPE = "PARTNER";

    private final PartnerMapper partnerMapper;
    private final AuditLogRecordService auditLogRecordService;

    public PartnersService(PartnerMapper partnerMapper,
                           AuditLogRecordService auditLogRecordService) {
        this.partnerMapper = partnerMapper;
        this.auditLogRecordService = auditLogRecordService;
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
        return partnerMapper.selectList(wrapper.orderByDesc(PartnerEntity::getId));
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
        auditLogRecordService.record(MODULE, status.equals("ACTIVE") ? "ENABLE" : "DISABLE", RESOURCE_TYPE,
                String.valueOf(entity.getId()), entity.getPartnerCode(), before, snapshot(entity));
        return Map.of("id", String.valueOf(entity.getId()), "status", entity.getStatus());
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

    private String snapshot(PartnerEntity entity) {
        return "{\"partnerCode\":\"" + safe(entity.getPartnerCode()) + "\",\"partnerName\":\"" + safe(entity.getPartnerName())
                + "\",\"partnerType\":\"" + safe(entity.getPartnerType()) + "\",\"status\":\"" + safe(entity.getStatus()) + "\"}";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "'");
    }
}
