package com.novadepot.backend.modules.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.model.entity.OutboundOrderItemEntity;
import com.novadepot.backend.model.entity.PartnerEntity;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.model.entity.SalesOrderItemEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.OutboundOrderItemMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.PartnerMapper;
import com.novadepot.backend.repository.SalesOrderItemMapper;
import com.novadepot.backend.repository.SalesOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesOrdersService {
    private static final String MODULE = "ERP_SALES";
    private static final String RESOURCE_TYPE = "SALES_ORDER";

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final PartnerMapper partnerMapper;
    private final AuditLogRecordService auditLogRecordService;

    public SalesOrdersService(SalesOrderMapper salesOrderMapper,
                              SalesOrderItemMapper salesOrderItemMapper,
                              OutboundOrderMapper outboundOrderMapper,
                              OutboundOrderItemMapper outboundOrderItemMapper,
                              InventoryMapper inventoryMapper,
                              PartnerMapper partnerMapper,
                              AuditLogRecordService auditLogRecordService) {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.partnerMapper = partnerMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<SalesOrderEntity> list(String status, Long partnerId) {
        LambdaQueryWrapper<SalesOrderEntity> wrapper = new LambdaQueryWrapper<SalesOrderEntity>()
                .eq(SalesOrderEntity::getTenantId, RequestContext.tenantId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(SalesOrderEntity::getStatus, status.trim().toUpperCase());
        }
        if (partnerId != null) {
            wrapper.eq(SalesOrderEntity::getPartnerId, partnerId);
        }
        return salesOrderMapper.selectList(wrapper.orderByDesc(SalesOrderEntity::getId));
    }

    public Map<String, Object> detail(Long id) {
        SalesOrderEntity order = mustGet(id);
        List<SalesOrderItemEntity> items = salesOrderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItemEntity>()
                .eq(SalesOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(SalesOrderItemEntity::getSalesOrderId, id)
                .orderByAsc(SalesOrderItemEntity::getLineNo));
        List<OutboundOrderEntity> linkedOutbounds = outboundOrderMapper.selectList(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(OutboundOrderEntity::getSourceType, "SALES_ORDER")
                .eq(OutboundOrderEntity::getSourceOrderId, id)
                .orderByDesc(OutboundOrderEntity::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("linkedOutbounds", linkedOutbounds);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(SalesOrderRequest req) {
        ensureSalesPartner(req.getPartnerId());
        SalesOrderEntity order = new SalesOrderEntity();
        order.setTenantId(RequestContext.tenantId());
        order.setSalesNo(NoGenerator.next("SO"));
        order.setStatus("DRAFT");
        apply(order, req);
        order.setCreatedBy(RequestContext.userId());
        order.setUpdatedBy(RequestContext.userId());
        salesOrderMapper.insert(order);
        rewriteItems(order, req);
        recordAudit(order, "CREATE", null, "DRAFT");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, SalesOrderRequest req) {
        SalesOrderEntity order = mustGet(id);
        ensureStatus(order, "DRAFT", "Only draft sales orders can be edited");
        ensureSalesPartner(req.getPartnerId());
        apply(order, req);
        order.setUpdatedBy(RequestContext.userId());
        salesOrderMapper.updateById(order);
        rewriteItems(order, req);
        recordAudit(order, "UPDATE", "DRAFT", "DRAFT");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long id) {
        SalesOrderEntity order = mustGet(id);
        ensureStatus(order, "DRAFT", "Only draft sales orders can be confirmed");
        order.setStatus("CONFIRMED");
        order.setUpdatedBy(RequestContext.userId());
        salesOrderMapper.updateById(order);
        recordAudit(order, "CONFIRM", "DRAFT", "CONFIRMED");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(Long id) {
        SalesOrderEntity order = mustGet(id);
        if (!List.of("DRAFT", "CONFIRMED").contains(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Only draft or confirmed sales orders can be cancelled");
        }
        ensureNoActiveOutbound(order);
        String before = order.getStatus();
        order.setStatus("CANCELLED");
        order.setUpdatedBy(RequestContext.userId());
        salesOrderMapper.updateById(order);
        recordAudit(order, "CANCEL", before, "CANCELLED");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOutboundDraft(Long id, Long locationId) {
        SalesOrderEntity order = mustGet(id);
        if (!List.of("CONFIRMED", "PARTIAL_SHIPPED").contains(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Only confirmed or partially shipped sales orders can create outbound drafts");
        }
        if (locationId == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Please select an outbound location");
        }
        ensureNoActiveOutbound(order);

        List<SalesOrderItemEntity> pendingItems = salesOrderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItemEntity>()
                        .eq(SalesOrderItemEntity::getTenantId, RequestContext.tenantId())
                        .eq(SalesOrderItemEntity::getSalesOrderId, id)
                        .orderByAsc(SalesOrderItemEntity::getLineNo))
                .stream()
                .filter(item -> item.getOrderQty().subtract(nullToZero(item.getShippedQty())).compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (pendingItems.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "This sales order has no pending outbound quantity");
        }
        ensureStockAvailable(order, locationId, pendingItems);

        OutboundOrderEntity outbound = new OutboundOrderEntity();
        outbound.setTenantId(RequestContext.tenantId());
        outbound.setOutboundNo(NoGenerator.next("OUT"));
        outbound.setBizType("SALES");
        outbound.setStatus("DRAFT");
        outbound.setSourceType("SALES_ORDER");
        outbound.setSourceOrderId(order.getId());
        outbound.setSourceOrderNo(order.getSalesNo());
        outbound.setWarehouseId(order.getWarehouseId());
        outbound.setCustomerId(order.getPartnerId());
        outbound.setCreatedBy(RequestContext.userId());
        outbound.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.insert(outbound);

        int lineNo = 1;
        for (SalesOrderItemEntity item : pendingItems) {
            BigDecimal pendingQty = item.getOrderQty().subtract(nullToZero(item.getShippedQty()));
            OutboundOrderItemEntity outboundItem = new OutboundOrderItemEntity();
            outboundItem.setTenantId(RequestContext.tenantId());
            outboundItem.setOutboundOrderId(outbound.getId());
            outboundItem.setLineNo(lineNo++);
            outboundItem.setSourceOrderItemId(item.getId());
            outboundItem.setSourceLineNo(item.getLineNo());
            outboundItem.setProductId(item.getProductId());
            outboundItem.setLocationId(locationId);
            outboundItem.setPlanQty(pendingQty);
            outboundItem.setPickedQty(BigDecimal.ZERO);
            outboundItem.setShippedQty(BigDecimal.ZERO);
            outboundItem.setUnitId(1L);
            outboundOrderItemMapper.insert(outboundItem);
        }

        recordConversionAudit(order, outbound);
        return Map.of("id", String.valueOf(outbound.getId()), "outboundNo", outbound.getOutboundNo(), "status", outbound.getStatus());
    }

    private void apply(SalesOrderEntity order, SalesOrderRequest req) {
        order.setPartnerId(req.getPartnerId());
        order.setCustomerId(req.getPartnerId());
        order.setWarehouseId(req.getWarehouseId());
        order.setDeliveryDate(req.getDeliveryDate());
        order.setRemark(StringUtils.hasText(req.getRemark()) ? req.getRemark().trim() : null);
        order.setTotalAmount(total(req.getItems()));
    }

    private void rewriteItems(SalesOrderEntity order, SalesOrderRequest req) {
        salesOrderItemMapper.delete(new LambdaQueryWrapper<SalesOrderItemEntity>()
                .eq(SalesOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(SalesOrderItemEntity::getSalesOrderId, order.getId()));
        int lineNo = 1;
        for (SalesOrderRequest.Item item : req.getItems()) {
            SalesOrderItemEntity entity = new SalesOrderItemEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setSalesOrderId(order.getId());
            entity.setLineNo(lineNo++);
            entity.setProductId(item.getProductId());
            entity.setOrderQty(item.getOrderQty());
            entity.setShippedQty(BigDecimal.ZERO);
            entity.setUnitPrice(item.getUnitPrice());
            entity.setTaxRate(BigDecimal.ZERO);
            salesOrderItemMapper.insert(entity);
        }
    }

    private BigDecimal total(List<SalesOrderRequest.Item> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(item.getOrderQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ensureSalesPartner(Long partnerId) {
        PartnerEntity partner = partnerMapper.selectOne(new LambdaQueryWrapper<PartnerEntity>()
                .eq(PartnerEntity::getTenantId, RequestContext.tenantId())
                .eq(PartnerEntity::getId, partnerId));
        if (partner == null || !"ACTIVE".equals(partner.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Partner does not exist or is disabled");
        }
        if (!List.of("CUSTOMER", "BOTH").contains(partner.getPartnerType())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Sales orders can only use customer or both-type partners");
        }
    }

    private void ensureStockAvailable(SalesOrderEntity order, Long locationId, List<SalesOrderItemEntity> pendingItems) {
        for (SalesOrderItemEntity item : pendingItems) {
            BigDecimal requiredQty = item.getOrderQty().subtract(nullToZero(item.getShippedQty()));
            InventoryEntity inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<InventoryEntity>()
                    .eq(InventoryEntity::getTenantId, RequestContext.tenantId())
                    .eq(InventoryEntity::getWarehouseId, order.getWarehouseId())
                    .eq(InventoryEntity::getLocationId, locationId)
                    .eq(InventoryEntity::getProductId, item.getProductId()));
            BigDecimal availableQty = inventory == null ? BigDecimal.ZERO : nullToZero(inventory.getAvailableQty());
            if (availableQty.compareTo(requiredQty) < 0) {
                BigDecimal shortage = requiredQty.subtract(availableQty);
                throw new BizException(ErrorCode.BIZ_ERROR.code(), "Insufficient stock: product " + item.getProductId()
                        + " available " + availableQty.stripTrailingZeros().toPlainString()
                        + ", required " + requiredQty.stripTrailingZeros().toPlainString()
                        + ", shortage " + shortage.stripTrailingZeros().toPlainString());
            }
        }
    }

    private SalesOrderEntity mustGet(Long id) {
        SalesOrderEntity order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrderEntity>()
                .eq(SalesOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(SalesOrderEntity::getId, id));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Sales order does not exist");
        }
        return order;
    }

    private void ensureStatus(SalesOrderEntity order, String status, String message) {
        if (!status.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
    }

    private void ensureNoActiveOutbound(SalesOrderEntity order) {
        Long count = outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(OutboundOrderEntity::getSourceType, "SALES_ORDER")
                .eq(OutboundOrderEntity::getSourceOrderId, order.getId())
                .ne(OutboundOrderEntity::getStatus, "CANCELED"));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "This sales order already has an active linked outbound order");
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> result(SalesOrderEntity order) {
        return Map.of("id", String.valueOf(order.getId()), "salesNo", order.getSalesNo(), "status", order.getStatus());
    }

    private void recordAudit(SalesOrderEntity order, String action, String before, String after) {
        String beforeJson = before == null ? null : "{\"status\":\"" + before + "\"}";
        String afterJson = "{\"status\":\"" + after + "\",\"partnerId\":\"" + order.getPartnerId() + "\",\"totalAmount\":\"" + order.getTotalAmount() + "\"}";
        auditLogRecordService.record(MODULE, action, RESOURCE_TYPE, String.valueOf(order.getId()), order.getSalesNo(), beforeJson, afterJson);
    }

    private void recordConversionAudit(SalesOrderEntity order, OutboundOrderEntity outbound) {
        String afterJson = "{\"status\":\"" + order.getStatus() + "\",\"outboundId\":\"" + outbound.getId()
                + "\",\"outboundNo\":\"" + outbound.getOutboundNo() + "\",\"sourceOrderNo\":\"" + order.getSalesNo() + "\"}";
        auditLogRecordService.record(MODULE, "CREATE_OUTBOUND_DRAFT", RESOURCE_TYPE, String.valueOf(order.getId()), order.getSalesNo(), null, afterJson);
        auditLogRecordService.record("WMS_OUTBOUND", "CREATE_FROM_SALES", "OUTBOUND_ORDER", String.valueOf(outbound.getId()), outbound.getOutboundNo(), null, afterJson);
    }
}
