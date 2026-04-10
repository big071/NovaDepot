package com.novadepot.backend.modules.warehouses;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.WarehouseEntity;
import com.novadepot.backend.repository.WarehouseMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WarehousesService {
    private final WarehouseMapper warehouseMapper;

    public WarehousesService(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
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

    public Map<String, Object> create(WarehouseCreateRequest req) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setWarehouseCode(req.getWarehouseCode());
        entity.setWarehouseName(req.getWarehouseName());
        entity.setWarehouseType(req.getWarehouseType() == null ? "STANDARD" : req.getWarehouseType());
        entity.setAddress(req.getAddress());
        entity.setStatus("ACTIVE");
        warehouseMapper.insert(entity);
        return Map.of("id", entity.getId(), "warehouseCode", entity.getWarehouseCode());
    }
}
