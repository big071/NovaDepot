package com.novadepot.backend.modules.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryService {
    private static final BigDecimal LOW_STOCK_THRESHOLD = BigDecimal.TEN;

    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;

    public InventoryService(InventoryMapper inventoryMapper,
                            InventoryTransactionMapper inventoryTransactionMapper) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
    }

    public List<InventoryEntity> list() {
        return inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(InventoryEntity::getId));
    }

    public List<InventoryTransactionEntity> transactions() {
        return inventoryTransactionMapper.selectList(new LambdaQueryWrapper<InventoryTransactionEntity>()
                .eq(InventoryTransactionEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(InventoryTransactionEntity::getOccurredAt)
                .last("limit 200"));
    }

    public List<InventoryEntity> lowStockAlerts() {
        return inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                .le(InventoryEntity::getAvailableQty, LOW_STOCK_THRESHOLD)
                .orderByAsc(InventoryEntity::getAvailableQty));
    }
}
