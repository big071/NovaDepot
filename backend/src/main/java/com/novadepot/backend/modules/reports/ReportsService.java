package com.novadepot.backend.modules.reports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.ProductMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ReportsService {
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;

    public ReportsService(ProductMapper productMapper,
                          InventoryMapper inventoryMapper,
                          InboundOrderMapper inboundOrderMapper,
                          OutboundOrderMapper outboundOrderMapper) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundOrderMapper = outboundOrderMapper;
    }

    public Map<String, Object> dashboard() {
        Long tenantId = RequestContext.tenantId();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long totalSku = productMapper.selectCount(new LambdaQueryWrapper<com.novadepot.backend.model.entity.ProductEntity>()
                .eq(com.novadepot.backend.model.entity.ProductEntity::getTenantId, tenantId));

        long todayInbound = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, tenantId)
                .ge(InboundOrderEntity::getCreatedAt, start)
                .lt(InboundOrderEntity::getCreatedAt, end));

        long todayOutbound = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, tenantId)
                .ge(OutboundOrderEntity::getCreatedAt, start)
                .lt(OutboundOrderEntity::getCreatedAt, end));

        long lowStockCount = inventoryMapper.selectCount(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, tenantId)
                .le(InventoryEntity::getAvailableQty, BigDecimal.TEN));

        return Map.of(
                "totalSku", totalSku,
                "todayInbound", todayInbound,
                "todayOutbound", todayOutbound,
                "lowStockCount", lowStockCount
        );
    }
}
