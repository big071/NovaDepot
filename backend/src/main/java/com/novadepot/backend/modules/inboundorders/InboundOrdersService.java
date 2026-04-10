package com.novadepot.backend.modules.inboundorders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.model.entity.InboundOrderItemEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.repository.InboundOrderItemMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InboundOrdersService {
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;

    public InboundOrdersService(InboundOrderMapper inboundOrderMapper,
                                InboundOrderItemMapper inboundOrderItemMapper,
                                InventoryMapper inventoryMapper,
                                InventoryTransactionMapper inventoryTransactionMapper) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
    }

    public List<InboundOrderEntity> list() {
        return inboundOrderMapper.selectList(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(InboundOrderEntity::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(InboundCreateRequest req) {
        InboundOrderEntity order = new InboundOrderEntity();
        order.setTenantId(RequestContext.tenantId());
        order.setInboundNo(NoGenerator.next("IN"));
        order.setBizType("PURCHASE");
        order.setStatus("DRAFT");
        order.setWarehouseId(req.getWarehouseId());
        order.setSupplierId(req.getSupplierId());
        inboundOrderMapper.insert(order);

        int lineNo = 1;
        for (InboundCreateRequest.Item item : req.getItems()) {
            InboundOrderItemEntity entity = new InboundOrderItemEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setInboundOrderId(order.getId());
            entity.setLineNo(lineNo++);
            entity.setProductId(item.getProductId());
            entity.setLocationId(item.getLocationId());
            entity.setPlanQty(item.getQty());
            entity.setReceivedQty(BigDecimal.ZERO);
            entity.setQualifiedQty(BigDecimal.ZERO);
            entity.setUnitId(1L);
            inboundOrderItemMapper.insert(entity);
        }
        return Map.of("id", order.getId(), "inboundNo", order.getInboundNo(), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long id) {
        InboundOrderEntity order = mustGet(id);
        if (!"DRAFT".equals(order.getStatus()) && !"SUBMITTED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "当前状态不允许审核");
        }
        order.setStatus("APPROVED");
        inboundOrderMapper.updateById(order);
        return Map.of("id", id, "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> post(Long id) {
        InboundOrderEntity order = mustGet(id);
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "仅已审核单据可入账");
        }

        List<InboundOrderItemEntity> items = inboundOrderItemMapper.selectList(new LambdaQueryWrapper<InboundOrderItemEntity>()
                .eq(InboundOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(InboundOrderItemEntity::getInboundOrderId, id));

        for (InboundOrderItemEntity item : items) {
            BigDecimal qty = item.getPlanQty();
            InventoryEntity inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<InventoryEntity>()
                    .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                    .eq(InventoryEntity::getWarehouseId, order.getWarehouseId())
                    .eq(InventoryEntity::getLocationId, item.getLocationId())
                    .eq(InventoryEntity::getProductId, item.getProductId()));

            if (inventory == null) {
                inventory = new InventoryEntity();
                inventory.setTenantId(RequestContext.tenantId());
                inventory.setWarehouseId(order.getWarehouseId());
                inventory.setLocationId(item.getLocationId());
                inventory.setProductId(item.getProductId());
                inventory.setAvailableQty(BigDecimal.ZERO);
                inventory.setLockedQty(BigDecimal.ZERO);
                inventory.setInTransitQty(BigDecimal.ZERO);
                inventory.setVersionNo(0);
                inventoryMapper.insert(inventory);
            }

            BigDecimal before = inventory.getAvailableQty();
            BigDecimal after = before.add(qty);
            inventory.setAvailableQty(after);
            inventoryMapper.updateById(inventory);

            InventoryTransactionEntity txn = new InventoryTransactionEntity();
            txn.setTenantId(RequestContext.tenantId());
            txn.setTxnNo(NoGenerator.next("TXN"));
            txn.setBizType("INBOUND");
            txn.setBizNo(order.getInboundNo());
            txn.setWarehouseId(order.getWarehouseId());
            txn.setLocationId(item.getLocationId());
            txn.setProductId(item.getProductId());
            txn.setChangeQty(qty);
            txn.setBeforeQty(before);
            txn.setAfterQty(after);
            txn.setOperatorId(RequestContext.userId());
            txn.setOccurredAt(LocalDateTime.now());
            inventoryTransactionMapper.insert(txn);

            item.setReceivedQty(qty);
            item.setQualifiedQty(qty);
            inboundOrderItemMapper.updateById(item);
        }

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        inboundOrderMapper.updateById(order);

        return Map.of("id", id, "status", order.getStatus(), "inventoryPosted", true);
    }

    private InboundOrderEntity mustGet(Long id) {
        InboundOrderEntity order = inboundOrderMapper.selectOne(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(InboundOrderEntity::getId, id));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "入库单不存在");
        }
        return order;
    }
}
