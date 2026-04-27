package com.novadepot.backend.modules.locations;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.WarehouseLocationEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.WarehouseLocationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LocationsService {
    private final WarehouseLocationMapper locationMapper;
    private final AuditLogRecordService auditLogRecordService;

    public LocationsService(WarehouseLocationMapper locationMapper,
                            AuditLogRecordService auditLogRecordService) {
        this.locationMapper = locationMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<WarehouseLocationEntity> list(Long warehouseId) {
        LambdaQueryWrapper<WarehouseLocationEntity> qw = new LambdaQueryWrapper<WarehouseLocationEntity>()
                .eq(WarehouseLocationEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(WarehouseLocationEntity::getId);
        if (warehouseId != null) {
            qw.eq(WarehouseLocationEntity::getWarehouseId, warehouseId);
        }
        return locationMapper.selectList(qw);
    }

    public WarehouseLocationEntity detail(Long id) {
        return locationMapper.selectOne(new LambdaQueryWrapper<WarehouseLocationEntity>()
                .eq(WarehouseLocationEntity::getTenantId, RequestContext.tenantId())
                .eq(WarehouseLocationEntity::getId, id));
    }

    public WarehouseLocationEntity detailByCode(String locationCode) {
        return locationMapper.selectOne(new LambdaQueryWrapper<WarehouseLocationEntity>()
                .eq(WarehouseLocationEntity::getTenantId, RequestContext.tenantId())
                .eq(WarehouseLocationEntity::getLocationCode, locationCode));
    }

    public Map<String, Object> create(LocationCreateRequest req) {
        validateUniqueCode(req.getWarehouseId(), req.getLocationCode(), null);
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setLocationCode(req.getLocationCode());
        entity.setLocationName(req.getLocationName());
        entity.setLocationType(req.getLocationType() == null ? "NORMAL" : req.getLocationType());
        entity.setCapacityQty(req.getCapacityQty());
        entity.setStatus("ACTIVE");
        locationMapper.insert(entity);
        auditLogRecordService.record("LOCATION", "CREATE", "LOCATION", String.valueOf(entity.getId()),
                entity.getLocationCode(), null, "{\"locationName\":\"" + safe(entity.getLocationName()) + "\"}");
        return Map.of("id", entity.getId(), "locationCode", entity.getLocationCode());
    }

    public Map<String, Object> update(Long id, LocationCreateRequest req) {
        WarehouseLocationEntity entity = detail(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Location not found");
        }
        validateUniqueCode(req.getWarehouseId(), req.getLocationCode(), entity.getId());
        String before = "{\"locationName\":\"" + safe(entity.getLocationName()) + "\",\"capacityQty\":\"" + entity.getCapacityQty() + "\"}";
        entity.setWarehouseId(req.getWarehouseId());
        entity.setLocationCode(req.getLocationCode());
        entity.setLocationName(req.getLocationName());
        entity.setLocationType(req.getLocationType() == null ? "NORMAL" : req.getLocationType());
        entity.setCapacityQty(req.getCapacityQty());
        locationMapper.updateById(entity);
        String after = "{\"locationName\":\"" + safe(entity.getLocationName()) + "\",\"capacityQty\":\"" + entity.getCapacityQty() + "\"}";
        auditLogRecordService.record("LOCATION", "UPDATE", "LOCATION", String.valueOf(entity.getId()),
                entity.getLocationCode(), before, after);
        return Map.of("id", entity.getId(), "locationCode", entity.getLocationCode());
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private void validateUniqueCode(Long warehouseId, String locationCode, Long selfId) {
        if (warehouseId == null || locationCode == null || locationCode.isBlank()) {
            return;
        }
        WarehouseLocationEntity same = locationMapper.selectOne(new LambdaQueryWrapper<WarehouseLocationEntity>()
                .eq(WarehouseLocationEntity::getTenantId, RequestContext.tenantId())
                .eq(WarehouseLocationEntity::getWarehouseId, warehouseId)
                .eq(WarehouseLocationEntity::getLocationCode, locationCode));
        if (same == null) {
            return;
        }
        if (selfId != null && selfId.equals(same.getId())) {
            return;
        }
        throw new BizException(ErrorCode.BIZ_ERROR.code(), "locationCode already exists in warehouse");
    }
}
