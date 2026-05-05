package com.novadepot.backend.modules.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.PartnerEntity;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.model.entity.SalesOrderItemEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
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
    private final PartnerMapper partnerMapper;
    private final AuditLogRecordService auditLogRecordService;

    public SalesOrdersService(SalesOrderMapper salesOrderMapper,
                              SalesOrderItemMapper salesOrderItemMapper,
                              PartnerMapper partnerMapper,
                              AuditLogRecordService auditLogRecordService) {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
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
        ensureStatus(order, "DRAFT", "仅草稿销售单可编辑");
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
        ensureStatus(order, "DRAFT", "仅草稿销售单可确认");
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
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "仅草稿或已确认销售单可取消");
        }
        String before = order.getStatus();
        order.setStatus("CANCELLED");
        order.setUpdatedBy(RequestContext.userId());
        salesOrderMapper.updateById(order);
        recordAudit(order, "CANCEL", before, "CANCELLED");
        return result(order);
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
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "往来单位不存在或已停用");
        }
        if (!List.of("CUSTOMER", "BOTH").contains(partner.getPartnerType())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "销售单只能选择客户或双向往来单位");
        }
    }

    private SalesOrderEntity mustGet(Long id) {
        SalesOrderEntity order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrderEntity>()
                .eq(SalesOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(SalesOrderEntity::getId, id));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "销售单不存在");
        }
        return order;
    }

    private void ensureStatus(SalesOrderEntity order, String status, String message) {
        if (!status.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
    }

    private Map<String, Object> result(SalesOrderEntity order) {
        return Map.of("id", String.valueOf(order.getId()), "salesNo", order.getSalesNo(), "status", order.getStatus());
    }

    private void recordAudit(SalesOrderEntity order, String action, String before, String after) {
        String beforeJson = before == null ? null : "{\"status\":\"" + before + "\"}";
        String afterJson = "{\"status\":\"" + after + "\",\"partnerId\":\"" + order.getPartnerId() + "\",\"totalAmount\":\"" + order.getTotalAmount() + "\"}";
        auditLogRecordService.record(MODULE, action, RESOURCE_TYPE, String.valueOf(order.getId()), order.getSalesNo(), beforeJson, afterJson);
    }
}
