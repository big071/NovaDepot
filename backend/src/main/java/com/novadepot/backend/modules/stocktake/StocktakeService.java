package com.novadepot.backend.modules.stocktake;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.model.entity.StocktakeOrderEntity;
import com.novadepot.backend.model.entity.StocktakeOrderItemEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.StocktakeOrderItemMapper;
import com.novadepot.backend.repository.StocktakeOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StocktakeService {
    private static final String MODULE = "WMS_STOCKTAKE";
    private static final String RESOURCE_TYPE = "STOCKTAKE_ORDER";

    private final StocktakeOrderMapper stocktakeOrderMapper;
    private final StocktakeOrderItemMapper stocktakeOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final AuditLogRecordService auditLogRecordService;

    public StocktakeService(StocktakeOrderMapper stocktakeOrderMapper,
                            StocktakeOrderItemMapper stocktakeOrderItemMapper,
                            InventoryMapper inventoryMapper,
                            InventoryTransactionMapper inventoryTransactionMapper,
                            AuditLogRecordService auditLogRecordService) {
        this.stocktakeOrderMapper = stocktakeOrderMapper;
        this.stocktakeOrderItemMapper = stocktakeOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<StocktakeOrderEntity> list(String status) {
        LambdaQueryWrapper<StocktakeOrderEntity> wrapper = new LambdaQueryWrapper<StocktakeOrderEntity>()
                .eq(StocktakeOrderEntity::getTenantId, RequestContext.tenantId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(StocktakeOrderEntity::getStatus, status.trim().toUpperCase());
        }
        return stocktakeOrderMapper.selectList(wrapper.orderByDesc(StocktakeOrderEntity::getId));
    }

    public Map<String, Object> detail(Long id) {
        StocktakeOrderEntity order = mustGet(id);
        List<StocktakeOrderItemEntity> items = stocktakeOrderItemMapper.selectList(new LambdaQueryWrapper<StocktakeOrderItemEntity>()
                .eq(StocktakeOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(StocktakeOrderItemEntity::getStocktakeOrderId, id)
                .orderByAsc(StocktakeOrderItemEntity::getLineNo));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(StocktakeCreateRequest request) {
        StocktakeOrderEntity order = new StocktakeOrderEntity();
        order.setTenantId(RequestContext.tenantId());
        order.setStocktakeNo(NoGenerator.next("ST"));
        order.setStatus("DRAFT");
        order.setWarehouseId(request.getWarehouseId());
        order.setScopeType("WAREHOUSE");
        order.setPlannedAt(LocalDateTime.now());
        order.setDiffCount(0);
        order.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        order.setCreatedBy(RequestContext.userId());
        order.setUpdatedBy(RequestContext.userId());
        stocktakeOrderMapper.insert(order);
        record(order, "CREATE", null, "DRAFT", "{\"warehouseId\":\"" + order.getWarehouseId() + "\"}");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> start(Long id) {
        StocktakeOrderEntity order = mustGet(id);
        ensureStatus(order, "DRAFT", "Only draft stocktake orders can be started");

        List<InventoryEntity> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                .eq(InventoryEntity::getWarehouseId, order.getWarehouseId())
                .orderByAsc(InventoryEntity::getLocationId)
                .orderByAsc(InventoryEntity::getProductId));
        if (inventories.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "No inventory rows can be counted in this warehouse");
        }

        stocktakeOrderItemMapper.delete(new LambdaQueryWrapper<StocktakeOrderItemEntity>()
                .eq(StocktakeOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(StocktakeOrderItemEntity::getStocktakeOrderId, order.getId()));
        int lineNo = 1;
        for (InventoryEntity inventory : inventories) {
            StocktakeOrderItemEntity item = new StocktakeOrderItemEntity();
            item.setTenantId(RequestContext.tenantId());
            item.setStocktakeOrderId(order.getId());
            item.setLineNo(lineNo++);
            item.setProductId(inventory.getProductId());
            item.setLocationId(inventory.getLocationId());
            item.setSystemQty(nullToZero(inventory.getAvailableQty()));
            item.setCountedQty(nullToZero(inventory.getAvailableQty()));
            item.setDiffQty(BigDecimal.ZERO);
            item.setResultType("PENDING");
            item.setCreatedBy(RequestContext.userId());
            item.setUpdatedBy(RequestContext.userId());
            stocktakeOrderItemMapper.insert(item);
        }

        order.setStatus("IN_PROGRESS");
        order.setStartedAt(LocalDateTime.now());
        order.setDiffCount(0);
        order.setUpdatedBy(RequestContext.userId());
        stocktakeOrderMapper.updateById(order);
        record(order, "START", "DRAFT", "IN_PROGRESS", "{\"itemCount\":" + inventories.size() + "}");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateCount(Long id, Long itemId, StocktakeCountRequest request) {
        StocktakeOrderEntity order = mustGet(id);
        ensureStatus(order, "IN_PROGRESS", "Only in-progress stocktake orders can be counted");
        StocktakeOrderItemEntity item = mustGetItem(id, itemId);
        BigDecimal counted = nullToZero(request.getCountedQty());
        BigDecimal diff = counted.subtract(nullToZero(item.getSystemQty()));
        item.setCountedQty(counted);
        item.setDiffQty(diff);
        item.setResultType(diff.compareTo(BigDecimal.ZERO) == 0 ? "MATCH" : "DIFF");
        item.setUpdatedBy(RequestContext.userId());
        stocktakeOrderItemMapper.updateById(item);
        record(order, "COUNT_UPDATE", "IN_PROGRESS", "IN_PROGRESS",
                "{\"itemId\":\"" + item.getId() + "\",\"countedQty\":\"" + counted + "\",\"diffQty\":\"" + diff + "\"}");
        return Map.of("id", String.valueOf(item.getId()), "diffQty", diff, "resultType", item.getResultType());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitReview(Long id) {
        StocktakeOrderEntity order = mustGet(id);
        ensureStatus(order, "IN_PROGRESS", "Only in-progress stocktake orders can be submitted for review");
        Long pending = stocktakeOrderItemMapper.selectCount(new LambdaQueryWrapper<StocktakeOrderItemEntity>()
                .eq(StocktakeOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(StocktakeOrderItemEntity::getStocktakeOrderId, id)
                .eq(StocktakeOrderItemEntity::getResultType, "PENDING"));
        if (pending != null && pending > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "All stocktake items must be counted before review");
        }
        Integer diffCount = Math.toIntExact(stocktakeOrderItemMapper.selectCount(new LambdaQueryWrapper<StocktakeOrderItemEntity>()
                .eq(StocktakeOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(StocktakeOrderItemEntity::getStocktakeOrderId, id)
                .ne(StocktakeOrderItemEntity::getDiffQty, BigDecimal.ZERO)));
        order.setStatus("DIFF_REVIEW");
        order.setDiffCount(diffCount);
        order.setUpdatedBy(RequestContext.userId());
        stocktakeOrderMapper.updateById(order);
        record(order, "SUBMIT_REVIEW", "IN_PROGRESS", "DIFF_REVIEW", "{\"diffCount\":" + diffCount + "}");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long id) {
        StocktakeOrderEntity order = mustGet(id);
        ensureStatus(order, "DIFF_REVIEW", "Only difference-review stocktake orders can be confirmed");
        List<StocktakeOrderItemEntity> items = stocktakeOrderItemMapper.selectList(new LambdaQueryWrapper<StocktakeOrderItemEntity>()
                .eq(StocktakeOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(StocktakeOrderItemEntity::getStocktakeOrderId, id)
                .orderByAsc(StocktakeOrderItemEntity::getLineNo));

        int adjustedCount = 0;
        for (StocktakeOrderItemEntity item : items) {
            BigDecimal diff = nullToZero(item.getDiffQty());
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            InventoryEntity inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<InventoryEntity>()
                    .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                    .eq(InventoryEntity::getWarehouseId, order.getWarehouseId())
                    .eq(InventoryEntity::getLocationId, item.getLocationId())
                    .eq(InventoryEntity::getProductId, item.getProductId()));
            if (inventory == null) {
                throw new BizException(ErrorCode.BIZ_ERROR.code(), "Inventory row no longer exists for stocktake item " + item.getId());
            }
            BigDecimal before = nullToZero(inventory.getAvailableQty());
            if (before.compareTo(nullToZero(item.getSystemQty())) != 0) {
                throw new BizException(ErrorCode.BIZ_ERROR.code(), "Inventory changed after stocktake snapshot. Restart stocktake before confirming.");
            }
            BigDecimal after = before.add(diff);
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(ErrorCode.BIZ_ERROR.code(), "Stocktake adjustment would make inventory negative");
            }
            InventoryTransactionEntity txn = new InventoryTransactionEntity();
            txn.setTenantId(RequestContext.tenantId());
            txn.setTxnNo(NoGenerator.next("TXN"));
            txn.setBizType("STOCKTAKE_ADJUST");
            txn.setBizNo(order.getStocktakeNo());
            txn.setWarehouseId(order.getWarehouseId());
            txn.setLocationId(item.getLocationId());
            txn.setProductId(item.getProductId());
            txn.setChangeQty(diff);
            txn.setBeforeQty(before);
            txn.setAfterQty(after);
            txn.setRequestId(order.getStocktakeNo() + "-" + item.getId());
            txn.setOperatorId(RequestContext.userId());
            txn.setOccurredAt(LocalDateTime.now());
            txn.setCreatedBy(RequestContext.userId());
            txn.setUpdatedBy(RequestContext.userId());
            inventoryTransactionMapper.insert(txn);

            inventory.setAvailableQty(after);
            inventory.setVersionNo(inventory.getVersionNo() == null ? 1 : inventory.getVersionNo() + 1);
            inventory.setUpdatedBy(RequestContext.userId());
            inventoryMapper.updateById(inventory);
            adjustedCount++;

            auditLogRecordService.record("INVENTORY", "STOCKTAKE_ADJUST", "INVENTORY", String.valueOf(inventory.getId()),
                    order.getStocktakeNo(), "{\"availableQty\":\"" + before + "\"}",
                    "{\"availableQty\":\"" + after + "\",\"changeQty\":\"" + diff + "\",\"stocktakeNo\":\"" + order.getStocktakeNo() + "\"}");
        }

        order.setStatus("COMPLETED");
        order.setFinishedAt(LocalDateTime.now());
        order.setUpdatedBy(RequestContext.userId());
        stocktakeOrderMapper.updateById(order);
        record(order, "CONFIRM_DIFF", "DIFF_REVIEW", "COMPLETED", "{\"adjustedCount\":" + adjustedCount + "}");
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus(), "adjustedCount", adjustedCount);
    }

    private StocktakeOrderEntity mustGet(Long id) {
        StocktakeOrderEntity order = stocktakeOrderMapper.selectOne(new LambdaQueryWrapper<StocktakeOrderEntity>()
                .eq(StocktakeOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(StocktakeOrderEntity::getId, id));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Stocktake order does not exist");
        }
        return order;
    }

    private StocktakeOrderItemEntity mustGetItem(Long orderId, Long itemId) {
        StocktakeOrderItemEntity item = stocktakeOrderItemMapper.selectOne(new LambdaQueryWrapper<StocktakeOrderItemEntity>()
                .eq(StocktakeOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(StocktakeOrderItemEntity::getStocktakeOrderId, orderId)
                .eq(StocktakeOrderItemEntity::getId, itemId));
        if (item == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Stocktake item does not exist");
        }
        return item;
    }

    private void ensureStatus(StocktakeOrderEntity order, String status, String message) {
        if (!status.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> result(StocktakeOrderEntity order) {
        return Map.of("id", String.valueOf(order.getId()), "stocktakeNo", order.getStocktakeNo(), "status", order.getStatus());
    }

    private void record(StocktakeOrderEntity order, String action, String before, String after, String extraJson) {
        String beforeJson = before == null ? null : "{\"status\":\"" + before + "\"}";
        String afterJson = "{\"status\":\"" + after + "\",\"stocktakeNo\":\"" + order.getStocktakeNo() + "\",\"extra\":" + extraJson + "}";
        auditLogRecordService.record(MODULE, action, RESOURCE_TYPE, String.valueOf(order.getId()), order.getStocktakeNo(), beforeJson, afterJson);
    }
}
