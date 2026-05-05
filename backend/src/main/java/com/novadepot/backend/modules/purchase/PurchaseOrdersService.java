package com.novadepot.backend.modules.purchase;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.PartnerEntity;
import com.novadepot.backend.model.entity.PurchaseOrderEntity;
import com.novadepot.backend.model.entity.PurchaseOrderItemEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
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
    private final PartnerMapper partnerMapper;
    private final AuditLogRecordService auditLogRecordService;

    public PurchaseOrdersService(PurchaseOrderMapper purchaseOrderMapper,
                                 PurchaseOrderItemMapper purchaseOrderItemMapper,
                                 PartnerMapper partnerMapper,
                                 AuditLogRecordService auditLogRecordService) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.partnerMapper = partnerMapper;
        this.auditLogRecordService = auditLogRecordService;
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
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
        ensureStatus(order, "DRAFT", "仅草稿采购单可编辑");
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
        ensureStatus(order, "DRAFT", "仅草稿采购单可确认");
        order.setStatus("CONFIRMED");
        order.setUpdatedBy(RequestContext.userId());
        purchaseOrderMapper.updateById(order);
        recordAudit(order, "CONFIRM", "DRAFT", "CONFIRMED");
        return result(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(Long id) {
        PurchaseOrderEntity order = mustGet(id);
        if (!List.of("DRAFT", "CONFIRMED").contains(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "仅草稿或已确认采购单可取消");
        }
        String before = order.getStatus();
        order.setStatus("CANCELLED");
        order.setUpdatedBy(RequestContext.userId());
        purchaseOrderMapper.updateById(order);
        recordAudit(order, "CANCEL", before, "CANCELLED");
        return result(order);
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
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "往来单位不存在或已停用");
        }
        if (!List.of("SUPPLIER", "BOTH").contains(partner.getPartnerType())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "采购单只能选择供应商或双向往来单位");
        }
    }

    private PurchaseOrderEntity mustGet(Long id) {
        PurchaseOrderEntity order = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(PurchaseOrderEntity::getId, id));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "采购单不存在");
        }
        return order;
    }

    private void ensureStatus(PurchaseOrderEntity order, String status, String message) {
        if (!status.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
    }

    private Map<String, Object> result(PurchaseOrderEntity order) {
        return Map.of("id", String.valueOf(order.getId()), "purchaseNo", order.getPurchaseNo(), "status", order.getStatus());
    }

    private void recordAudit(PurchaseOrderEntity order, String action, String before, String after) {
        String beforeJson = before == null ? null : "{\"status\":\"" + before + "\"}";
        String afterJson = "{\"status\":\"" + after + "\",\"partnerId\":\"" + order.getPartnerId() + "\",\"totalAmount\":\"" + order.getTotalAmount() + "\"}";
        auditLogRecordService.record(MODULE, action, RESOURCE_TYPE, String.valueOf(order.getId()), order.getPurchaseNo(), beforeJson, afterJson);
    }
}
