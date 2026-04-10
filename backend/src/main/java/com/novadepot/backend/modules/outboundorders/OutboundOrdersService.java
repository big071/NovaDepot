package com.novadepot.backend.modules.outboundorders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.model.entity.OutboundOrderItemEntity;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.OutboundOrderItemMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OutboundOrdersService {
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;

    public OutboundOrdersService(OutboundOrderMapper outboundOrderMapper,
                                 OutboundOrderItemMapper outboundOrderItemMapper,
                                 InventoryMapper inventoryMapper,
                                 InventoryTransactionMapper inventoryTransactionMapper) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
    }

    public List<OutboundOrderEntity> list() {
        return outboundOrderMapper.selectList(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(OutboundOrderEntity::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(OutboundCreateRequest req) {
        OutboundOrderEntity order = new OutboundOrderEntity();
        order.setTenantId(RequestContext.tenantId());
        order.setOutboundNo(NoGenerator.next("OUT"));
        order.setBizType("SALES");
        order.setStatus("DRAFT");
        order.setWarehouseId(req.getWarehouseId());
        order.setCustomerId(req.getCustomerId());
        outboundOrderMapper.insert(order);

        int lineNo = 1;
        for (OutboundCreateRequest.Item item : req.getItems()) {
            OutboundOrderItemEntity entity = new OutboundOrderItemEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setOutboundOrderId(order.getId());
            entity.setLineNo(lineNo++);
            entity.setProductId(item.getProductId());
            entity.setLocationId(item.getLocationId());
            entity.setPlanQty(item.getQty());
            entity.setPickedQty(BigDecimal.ZERO);
            entity.setShippedQty(BigDecimal.ZERO);
            entity.setUnitId(1L);
            outboundOrderItemMapper.insert(entity);
        }
        return Map.of("id", order.getId(), "outboundNo", order.getOutboundNo(), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long id) {
        OutboundOrderEntity order = mustGet(id);
        if (!"DRAFT".equals(order.getStatus()) && !"SUBMITTED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "当前状态不允许审核");
        }
        order.setStatus("APPROVED");
        outboundOrderMapper.updateById(order);
        return Map.of("id", id, "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ship(Long id) {
        OutboundOrderEntity order = mustGet(id);
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "仅已审核单据可出库");
        }

        List<OutboundOrderItemEntity> items = outboundOrderItemMapper.selectList(new LambdaQueryWrapper<OutboundOrderItemEntity>()
                .eq(OutboundOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(OutboundOrderItemEntity::getOutboundOrderId, id));

        for (OutboundOrderItemEntity item : items) {
            BigDecimal qty = item.getPlanQty();
            InventoryEntity inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<InventoryEntity>()
                    .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                    .eq(InventoryEntity::getWarehouseId, order.getWarehouseId())
                    .eq(InventoryEntity::getLocationId, item.getLocationId())
                    .eq(InventoryEntity::getProductId, item.getProductId()));

            if (inventory == null || inventory.getAvailableQty().compareTo(qty) < 0) {
                throw new BizException(ErrorCode.BIZ_ERROR.code(), "库存不足，无法出库");
            }

            BigDecimal before = inventory.getAvailableQty();
            BigDecimal after = before.subtract(qty);
            inventory.setAvailableQty(after);
            inventoryMapper.updateById(inventory);

            InventoryTransactionEntity txn = new InventoryTransactionEntity();
            txn.setTenantId(RequestContext.tenantId());
            txn.setTxnNo(NoGenerator.next("TXN"));
            txn.setBizType("OUTBOUND");
            txn.setBizNo(order.getOutboundNo());
            txn.setWarehouseId(order.getWarehouseId());
            txn.setLocationId(item.getLocationId());
            txn.setProductId(item.getProductId());
            txn.setChangeQty(qty.negate());
            txn.setBeforeQty(before);
            txn.setAfterQty(after);
            txn.setOperatorId(RequestContext.userId());
            txn.setOccurredAt(LocalDateTime.now());
            inventoryTransactionMapper.insert(txn);

            item.setPickedQty(qty);
            item.setShippedQty(qty);
            outboundOrderItemMapper.updateById(item);
        }

        order.setStatus("COMPLETED");
        order.setShippedAt(LocalDateTime.now());
        outboundOrderMapper.updateById(order);

        return Map.of("id", id, "status", order.getStatus(), "inventoryDeducted", true);
    }

    private OutboundOrderEntity mustGet(Long id) {
        OutboundOrderEntity order = outboundOrderMapper.selectOne(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(OutboundOrderEntity::getId, id));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "出库单不存在");
        }
        return order;
    }
}
