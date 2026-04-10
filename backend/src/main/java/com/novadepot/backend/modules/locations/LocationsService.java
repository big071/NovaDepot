package com.novadepot.backend.modules.locations;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.WarehouseLocationEntity;
import com.novadepot.backend.repository.WarehouseLocationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LocationsService {
    private final WarehouseLocationMapper locationMapper;

    public LocationsService(WarehouseLocationMapper locationMapper) {
        this.locationMapper = locationMapper;
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

    public Map<String, Object> create(LocationCreateRequest req) {
        WarehouseLocationEntity entity = new WarehouseLocationEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setLocationCode(req.getLocationCode());
        entity.setLocationName(req.getLocationName());
        entity.setLocationType(req.getLocationType() == null ? "NORMAL" : req.getLocationType());
        entity.setCapacityQty(req.getCapacityQty());
        entity.setStatus("ACTIVE");
        locationMapper.insert(entity);
        return Map.of("id", entity.getId(), "locationCode", entity.getLocationCode());
    }
}
