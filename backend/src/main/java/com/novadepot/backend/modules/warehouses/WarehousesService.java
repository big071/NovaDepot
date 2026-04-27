package com.novadepot.backend.modules.warehouses;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.WarehouseEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.WarehouseMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WarehousesService {
    private final WarehouseMapper warehouseMapper;
    private final AuditLogRecordService auditLogRecordService;

    public WarehousesService(WarehouseMapper warehouseMapper,
                             AuditLogRecordService auditLogRecordService) {
        this.warehouseMapper = warehouseMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<WarehouseEntity> list() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(WarehouseEntity::getId));
    }

    public WarehouseEntity detail(Long id) {
        return warehouseMapper.selectOne(new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getTenantId, RequestContext.tenantId())
                .eq(WarehouseEntity::getId, id));
    }

    public WarehouseEntity detailByCode(String warehouseCode) {
        return warehouseMapper.selectOne(new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getTenantId, RequestContext.tenantId())
                .eq(WarehouseEntity::getWarehouseCode, warehouseCode));
    }

    public Map<String, Object> create(WarehouseCreateRequest req) {
        validateUniqueCode(req.getWarehouseCode(), null);
        WarehouseEntity entity = new WarehouseEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setWarehouseCode(req.getWarehouseCode());
        entity.setWarehouseName(req.getWarehouseName());
        entity.setWarehouseType(req.getWarehouseType() == null ? "STANDARD" : req.getWarehouseType());
        entity.setAddress(req.getAddress());
        entity.setStatus("ACTIVE");
        warehouseMapper.insert(entity);
        auditLogRecordService.record("WAREHOUSE", "CREATE", "WAREHOUSE", String.valueOf(entity.getId()),
                entity.getWarehouseCode(), null, "{\"warehouseName\":\"" + safe(entity.getWarehouseName()) + "\"}");
        return Map.of("id", entity.getId(), "warehouseCode", entity.getWarehouseCode());
    }

    public Map<String, Object> update(Long id, WarehouseCreateRequest req) {
        WarehouseEntity entity = detail(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Warehouse not found");
        }
        validateUniqueCode(req.getWarehouseCode(), entity.getId());
        String before = "{\"warehouseName\":\"" + safe(entity.getWarehouseName()) + "\",\"address\":\"" + safe(entity.getAddress()) + "\"}";
        entity.setWarehouseCode(req.getWarehouseCode());
        entity.setWarehouseName(req.getWarehouseName());
        entity.setWarehouseType(req.getWarehouseType() == null ? "STANDARD" : req.getWarehouseType());
        entity.setAddress(req.getAddress());
        warehouseMapper.updateById(entity);
        String after = "{\"warehouseName\":\"" + safe(entity.getWarehouseName()) + "\",\"address\":\"" + safe(entity.getAddress()) + "\"}";
        auditLogRecordService.record("WAREHOUSE", "UPDATE", "WAREHOUSE", String.valueOf(entity.getId()),
                entity.getWarehouseCode(), before, after);
        return Map.of("id", entity.getId(), "warehouseCode", entity.getWarehouseCode());
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private void validateUniqueCode(String warehouseCode, Long selfId) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            return;
        }
        WarehouseEntity same = warehouseMapper.selectOne(new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getTenantId, RequestContext.tenantId())
                .eq(WarehouseEntity::getWarehouseCode, warehouseCode));
        if (same == null) {
            return;
        }
        if (selfId != null && selfId.equals(same.getId())) {
            return;
        }
        throw new BizException(ErrorCode.BIZ_ERROR.code(), "warehouseCode already exists");
    }
}
