package com.novadepot.backend.modules.purchase;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.model.entity.InboundOrderItemEntity;
import com.novadepot.backend.model.entity.PartnerEntity;
import com.novadepot.backend.model.entity.PurchaseOrderEntity;
import com.novadepot.backend.model.entity.PurchaseOrderItemEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.finance.FinanceService;
import com.novadepot.backend.repository.InboundOrderItemMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.PartnerMapper;
import com.novadepot.backend.repository.PurchaseOrderItemMapper;
import com.novadepot.backend.repository.PurchaseOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseOrdersService {
    private static final String MODULE = "ERP_PURCHASE";
    private static final String RESOURCE_TYPE = "PURCHASE_ORDER";

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final PartnerMapper partnerMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final FinanceService financeService;

    public PurchaseOrdersService(PurchaseOrderMapper purchaseOrderMapper,
                                 PurchaseOrderItemMapper purchaseOrderItemMapper,
                                 InboundOrderMapper inboundOrderMapper,
                                 InboundOrderItemMapper inboundOrderItemMapper,
                                 PartnerMapper partnerMapper,
                                 AuditLogRecordService auditLogRecordService,
                                 FinanceService financeService) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.partnerMapper = partnerMapper;
        this.auditLogRecordService = auditLogRecordService;
        this.financeService = financeService;
    }

    public List<PurchaseOrderEntity> list(String status, Long partnerId) {
        LambdaQueryWrapper<PurchaseOrderEntity> wrapper = new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getTenantId, RequestContext.tenantId());
        if (StringUtils.hasText(status)) {
            wrapper.eq(PurchaseOrderEntity::getStatus, status.trim().toUpperCase());
        }
        if (partnerId != null) {
            wrapper.eq(PurchaseOrderEntity::getPartnerId, partnerId);
        }
        return purchaseOrderMapper.selectList(wrapper.orderByDesc(PurchaseOrderEntity::getId));
    }

    public Map<String, Object> detail(Long id) {
        PurchaseOrderEntity order = mustGet(id);
        List<PurchaseOrderItemEntity> items = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItemEntity>()
                .eq(PurchaseOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(PurchaseOrderItemEntity::getPurchaseOrderId, id)
                .orderByAsc(PurchaseOrderItemEntity::getLineNo));
        List<InboundOrderEntity> linkedInbounds = inboundOrderMapper.selectList(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(InboundOrderEntity::getSourceType, "PURCHASE_ORDER")
                .eq(InboundOrderEntity::getSourceOrderId, id)
                .orderByDesc(InboundOrderEntity::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("linkedInbounds", linkedInbounds);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(PurchaseOrderRequest req) {
        ensurePurchasePartner(req.getPartnerId());
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setTenantId(RequestContext.tenantId());
        order.setPurchaseNo(NoGenerator.next("PO"));
        order.setStatus("DRAFT");
        apply(order, req);
        order.setCreatedBy(RequestContext.userId());
        order.setUpdatedBy(RequestContext.userId());
        purchaseOrderMapper.insert(order);
        rewriteItems(order, req);
        recordAudit(order, "CREATE", null, "DRAFT");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, PurchaseOrderRequest req) {
        PurchaseOrderEntity order = mustGet(id);
        ensureStatus(order, "DRAFT", "Only draft purchase orders can be edited");
        ensurePurchasePartner(req.getPartnerId());
        apply(order, req);
        order.setUpdatedBy(RequestContext.userId());
        purchaseOrderMapper.updateById(order);
        rewriteItems(order, req);
        recordAudit(order, "UPDATE", "DRAFT", "DRAFT");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long id) {
        PurchaseOrderEntity order = mustGet(id);
        ensureStatus(order, "DRAFT", "Only draft purchase orders can be confirmed");
        order.setStatus("CONFIRMED");
        order.setUpdatedBy(RequestContext.userId());
        purchaseOrderMapper.updateById(order);
        recordAudit(order, "CONFIRM", "DRAFT", "CONFIRMED");
        financeService.ensurePayableForPurchase(order);
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(Long id) {
        PurchaseOrderEntity order = mustGet(id);
        if (!List.of("DRAFT", "CONFIRMED").contains(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Only draft or confirmed purchase orders can be cancelled");
        }
        ensureNoActiveInbound(order);
        String before = order.getStatus();
        order.setStatus("CANCELLED");
        order.setUpdatedBy(RequestContext.userId());
        purchaseOrderMapper.updateById(order);
        recordAudit(order, "CANCEL", before, "CANCELLED");
        financeService.cancelPayableForPurchase(order);
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createInboundDraft(Long id, Long locationId) {
        PurchaseOrderEntity order = mustGet(id);
        if (!List.of("CONFIRMED", "PARTIAL_RECEIVED").contains(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Only confirmed or partially received purchase orders can create inbound drafts");
        }
        if (locationId == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Please select an inbound location");
        }
        ensureNoActiveInbound(order);

        List<PurchaseOrderItemEntity> pendingItems = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItemEntity>()
                        .eq(PurchaseOrderItemEntity::getTenantId, RequestContext.tenantId())
                        .eq(PurchaseOrderItemEntity::getPurchaseOrderId, id)
                        .orderByAsc(PurchaseOrderItemEntity::getLineNo))
                .stream()
                .filter(item -> item.getOrderQty().subtract(nullToZero(item.getReceivedQty())).compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (pendingItems.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "This purchase order has no pending inbound quantity");
        }

        InboundOrderEntity inbound = new InboundOrderEntity();
        inbound.setTenantId(RequestContext.tenantId());
        inbound.setInboundNo(NoGenerator.next("IN"));
        inbound.setBizType("PURCHASE");
        inbound.setStatus("DRAFT");
        inbound.setSourceType("PURCHASE_ORDER");
        inbound.setSourceOrderId(order.getId());
        inbound.setSourceOrderNo(order.getPurchaseNo());
        inbound.setWarehouseId(order.getWarehouseId());
        inbound.setSupplierId(order.getPartnerId());
        inbound.setCreatedBy(RequestContext.userId());
        inbound.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.insert(inbound);

        int lineNo = 1;
        for (PurchaseOrderItemEntity item : pendingItems) {
            BigDecimal pendingQty = item.getOrderQty().subtract(nullToZero(item.getReceivedQty()));
            InboundOrderItemEntity inboundItem = new InboundOrderItemEntity();
            inboundItem.setTenantId(RequestContext.tenantId());
            inboundItem.setInboundOrderId(inbound.getId());
            inboundItem.setLineNo(lineNo++);
            inboundItem.setSourceOrderItemId(item.getId());
            inboundItem.setSourceLineNo(item.getLineNo());
            inboundItem.setProductId(item.getProductId());
            inboundItem.setLocationId(locationId);
            inboundItem.setPlanQty(pendingQty);
            inboundItem.setReceivedQty(BigDecimal.ZERO);
            inboundItem.setQualifiedQty(BigDecimal.ZERO);
            inboundItem.setUnitId(1L);
            inboundOrderItemMapper.insert(inboundItem);
        }

        recordConversionAudit(order, inbound);
        return Map.of("id", String.valueOf(inbound.getId()), "inboundNo", inbound.getInboundNo(), "status", inbound.getStatus());
    }

    private void apply(PurchaseOrderEntity order, PurchaseOrderRequest req) {
        order.setPartnerId(req.getPartnerId());
        order.setSupplierId(req.getPartnerId());
        order.setWarehouseId(req.getWarehouseId());
        order.setExpectedArrivalDate(req.getExpectedArrivalDate());
        order.setRemark(StringUtils.hasText(req.getRemark()) ? req.getRemark().trim() : null);
        order.setTotalAmount(total(req.getItems()));
    }

    private void rewriteItems(PurchaseOrderEntity order, PurchaseOrderRequest req) {
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItemEntity>()
                .eq(PurchaseOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(PurchaseOrderItemEntity::getPurchaseOrderId, order.getId()));
        int lineNo = 1;
        for (PurchaseOrderRequest.Item item : req.getItems()) {
            PurchaseOrderItemEntity entity = new PurchaseOrderItemEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setPurchaseOrderId(order.getId());
            entity.setLineNo(lineNo++);
            entity.setProductId(item.getProductId());
            entity.setOrderQty(item.getOrderQty());
            entity.setReceivedQty(BigDecimal.ZERO);
            entity.setUnitPrice(item.getUnitPrice());
            entity.setTaxRate(BigDecimal.ZERO);
            purchaseOrderItemMapper.insert(entity);
        }
    }

    private BigDecimal total(List<PurchaseOrderRequest.Item> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(item.getOrderQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ensurePurchasePartner(Long partnerId) {
        PartnerEntity partner = partnerMapper.selectOne(new LambdaQueryWrapper<PartnerEntity>()
                .eq(PartnerEntity::getTenantId, RequestContext.tenantId())
                .eq(PartnerEntity::getId, partnerId));
        if (partner == null || !"ACTIVE".equals(partner.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Partner does not exist or is disabled");
        }
        if (!List.of("SUPPLIER", "BOTH").contains(partner.getPartnerType())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Purchase orders can only use supplier or both-type partners");
        }
    }

    private PurchaseOrderEntity mustGet(Long id) {
        PurchaseOrderEntity order = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(PurchaseOrderEntity::getId, id));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Purchase order does not exist");
        }
        return order;
    }

    private void ensureStatus(PurchaseOrderEntity order, String status, String message) {
        if (!status.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
    }

    private void ensureNoActiveInbound(PurchaseOrderEntity order) {
        Long count = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(InboundOrderEntity::getSourceType, "PURCHASE_ORDER")
                .eq(InboundOrderEntity::getSourceOrderId, order.getId())
                .ne(InboundOrderEntity::getStatus, "CANCELED"));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "This purchase order already has an active linked inbound order");
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> result(PurchaseOrderEntity order) {
        return Map.of("id", String.valueOf(order.getId()), "purchaseNo", order.getPurchaseNo(), "status", order.getStatus());
    }

    private void recordAudit(PurchaseOrderEntity order, String action, String before, String after) {
        String beforeJson = before == null ? null : "{\"status\":\"" + before + "\"}";
        String afterJson = "{\"status\":\"" + after + "\",\"partnerId\":\"" + order.getPartnerId() + "\",\"totalAmount\":\"" + order.getTotalAmount() + "\"}";
        auditLogRecordService.record(MODULE, action, RESOURCE_TYPE, String.valueOf(order.getId()), order.getPurchaseNo(), beforeJson, afterJson);
    }

    private void recordConversionAudit(PurchaseOrderEntity order, InboundOrderEntity inbound) {
        String afterJson = "{\"status\":\"" + order.getStatus() + "\",\"inboundId\":\"" + inbound.getId()
                + "\",\"inboundNo\":\"" + inbound.getInboundNo() + "\",\"sourceOrderNo\":\"" + order.getPurchaseNo() + "\"}";
        auditLogRecordService.record(MODULE, "CREATE_INBOUND_DRAFT", RESOURCE_TYPE, String.valueOf(order.getId()), order.getPurchaseNo(), null, afterJson);
        auditLogRecordService.record("WMS_INBOUND", "CREATE_FROM_PURCHASE", "INBOUND_ORDER", String.valueOf(inbound.getId()), inbound.getInboundNo(), null, afterJson);
    }
}
