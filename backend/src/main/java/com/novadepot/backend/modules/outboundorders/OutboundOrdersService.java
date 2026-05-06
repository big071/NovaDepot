package com.novadepot.backend.modules.outboundorders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.AuditLogEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.model.entity.OutboundOrderEntity;
import com.novadepot.backend.model.entity.OutboundOrderItemEntity;
import com.novadepot.backend.model.entity.SalesOrderEntity;
import com.novadepot.backend.model.entity.SalesOrderItemEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.AuditLogMapper;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.OutboundOrderItemMapper;
import com.novadepot.backend.repository.OutboundOrderMapper;
import com.novadepot.backend.repository.SalesOrderItemMapper;
import com.novadepot.backend.repository.SalesOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OutboundOrdersService {
    private static final String MODULE = "WMS_OUTBOUND";
    private static final String RESOURCE_TYPE = "OUTBOUND_ORDER";

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final AuditLogMapper auditLogMapper;
    private final AuthQueryMapper authQueryMapper;
    private final ObjectMapper objectMapper;

    public OutboundOrdersService(OutboundOrderMapper outboundOrderMapper,
                                 OutboundOrderItemMapper outboundOrderItemMapper,
                                 InventoryMapper inventoryMapper,
                                 InventoryTransactionMapper inventoryTransactionMapper,
                                 SalesOrderMapper salesOrderMapper,
                                 SalesOrderItemMapper salesOrderItemMapper,
                                 AuditLogRecordService auditLogRecordService,
                                 AuditLogMapper auditLogMapper,
                                 AuthQueryMapper authQueryMapper,
                                 ObjectMapper objectMapper) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.auditLogRecordService = auditLogRecordService;
        this.auditLogMapper = auditLogMapper;
        this.authQueryMapper = authQueryMapper;
        this.objectMapper = objectMapper;
    }

    public List<OutboundOrderEntity> list() {
        return outboundOrderMapper.selectList(new LambdaQueryWrapper<OutboundOrderEntity>()
                .eq(OutboundOrderEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(OutboundOrderEntity::getId));
    }

    public List<Map<String, Object>> items(Long id) {
        mustGet(id);
        List<OutboundOrderItemEntity> rows = outboundOrderItemMapper.selectList(new LambdaQueryWrapper<OutboundOrderItemEntity>()
                .eq(OutboundOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(OutboundOrderItemEntity::getOutboundOrderId, id)
                .orderByAsc(OutboundOrderItemEntity::getLineNo));
        return rows.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId() == null ? null : String.valueOf(item.getId()));
            row.put("lineNo", item.getLineNo());
            row.put("productId", item.getProductId() == null ? null : String.valueOf(item.getProductId()));
            row.put("locationId", item.getLocationId() == null ? null : String.valueOf(item.getLocationId()));
            row.put("sourceOrderItemId", item.getSourceOrderItemId() == null ? null : String.valueOf(item.getSourceOrderItemId()));
            row.put("sourceLineNo", item.getSourceLineNo());
            row.put("planQty", item.getPlanQty());
            row.put("pickedQty", item.getPickedQty());
            row.put("shippedQty", item.getShippedQty());
            return row;
        }).toList();
    }

    public Map<String, Object> detail(Long id) {
        OutboundOrderEntity order = mustGet(id);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("order", order);
        detail.put("items", items(id));
        detail.put("timeline", loadTimeline(order));
        detail.put("auditQuery", Map.of(
                "bizNo", order.getOutboundNo(),
                "resourceType", RESOURCE_TYPE,
                "resourceId", String.valueOf(order.getId())
        ));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(OutboundCreateRequest req) {
        OutboundOrderEntity order = new OutboundOrderEntity();
        order.setTenantId(RequestContext.tenantId());
        order.setOutboundNo(NoGenerator.next("OUT"));
        order.setBizType("SALES");
        order.setStatus("DRAFT");
        order.setSourceType("MANUAL");
        order.setWarehouseId(req.getWarehouseId());
        order.setCustomerId(req.getCustomerId());
        order.setCreatedBy(RequestContext.userId());
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.insert(order);
        rewriteItems(order, req);
        recordAudit(order, "CREATE", null, "DRAFT", "创建出库单", "创建草稿");
        return Map.of("id", String.valueOf(order.getId()), "outboundNo", order.getOutboundNo(), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, OutboundCreateRequest req) {
        OutboundOrderEntity order = mustGet(id);
        ensureOneOfStatus(order, List.of("DRAFT", "REJECTED"), "仅草稿或驳回状态可编辑");
        String before = order.getStatus();
        order.setWarehouseId(req.getWarehouseId());
        order.setCustomerId(req.getCustomerId());
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        rewriteItems(order, req);
        recordAudit(order, "UPDATE", before, before, "编辑出库单", "更新明细与基础信息");
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(Long id, String note) {
        OutboundOrderEntity order = mustGet(id);
        ensureOneOfStatus(order, List.of("DRAFT", "REJECTED"), "仅草稿或驳回状态可提交");
        String before = order.getStatus();
        order.setStatus("SUBMITTED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        recordAudit(order, "SUBMIT", before, "SUBMITTED", "提交出库单", defaultNote(note, "提交审核"));
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> withdraw(Long id, String note) {
        OutboundOrderEntity order = mustGet(id);
        ensureStatus(order, "SUBMITTED", "仅已提交状态可撤回");
        order.setStatus("DRAFT");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        recordAudit(order, "WITHDRAW", "SUBMITTED", "DRAFT", "撤回出库单", defaultNote(note, "提交后撤回"));
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(Long id, String note) {
        OutboundOrderEntity order = mustGet(id);
        ensureOneOfStatus(order, List.of("DRAFT", "SUBMITTED", "REJECTED"), "仅草稿/已提交/驳回状态可作废");
        String before = order.getStatus();
        order.setStatus("CANCELED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        recordAudit(order, "CANCEL", before, "CANCELED", "作废出库单", defaultNote(note, "作废单据"));
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long id, String note) {
        OutboundOrderEntity order = mustGet(id);
        ensureStatus(order, "SUBMITTED", "仅已提交状态可审核");
        ensureCurrentUserIsAdmin();
        ensureReviewerSeparation(order.getCreatedBy());
        order.setStatus("APPROVED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        recordAudit(order, "APPROVE", "SUBMITTED", "APPROVED", "审核通过", defaultNote(note, "审核通过"));
        return Map.of("id", String.valueOf(id), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(Long id, String note) {
        OutboundOrderEntity order = mustGet(id);
        ensureStatus(order, "SUBMITTED", "仅已提交状态可驳回");
        ensureCurrentUserIsAdmin();
        ensureReviewerSeparation(order.getCreatedBy());
        order.setStatus("REJECTED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        recordAudit(order, "REJECT", "SUBMITTED", "REJECTED", "驳回单据", defaultNote(note, "驳回，请补充信息后重提"));
        return Map.of("id", String.valueOf(id), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> unapprove(Long id, String note) {
        OutboundOrderEntity order = mustGet(id);
        ensureStatus(order, "APPROVED", "仅审核通过状态可反审核");
        ensureCurrentUserIsAdmin();
        order.setStatus("SUBMITTED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        recordAudit(order, "UNAPPROVE", "APPROVED", "SUBMITTED", "反审核", defaultNote(note, "反审核，回到待审核状态"));
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ship(Long id, String note) {
        OutboundOrderEntity order = mustGet(id);
        ensureStatus(order, "APPROVED", "仅审核通过状态可发运");

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
                throw new BizException(ErrorCode.BIZ_ERROR.code(), "库存不足，无法发运");
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

        order.setStatus("SHIPPED");
        order.setShippedAt(LocalDateTime.now());
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        outboundOrderMapper.updateById(order);
        recordAudit(order, "SHIP", "APPROVED", "SHIPPED", "发运出库", defaultNote(note, "完成发运"));
        syncSalesAfterShip(order, items);

        return Map.of("id", String.valueOf(id), "status", order.getStatus(), "inventoryDeducted", true);
    }

    private void rewriteItems(OutboundOrderEntity order, OutboundCreateRequest req) {
        outboundOrderItemMapper.delete(new LambdaQueryWrapper<OutboundOrderItemEntity>()
                .eq(OutboundOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(OutboundOrderItemEntity::getOutboundOrderId, order.getId()));

        int lineNo = 1;
        for (OutboundCreateRequest.Item item : req.getItems()) {
            OutboundOrderItemEntity entity = new OutboundOrderItemEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setOutboundOrderId(order.getId());
            entity.setLineNo(lineNo++);
            entity.setSourceOrderItemId(null);
            entity.setSourceLineNo(null);
            entity.setProductId(item.getProductId());
            entity.setLocationId(item.getLocationId());
            entity.setPlanQty(item.getQty());
            entity.setPickedQty(BigDecimal.ZERO);
            entity.setShippedQty(BigDecimal.ZERO);
            entity.setUnitId(1L);
            outboundOrderItemMapper.insert(entity);
        }
    }

    private void ensureStatus(OutboundOrderEntity order, String expected, String message) {
        if (!expected.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
    }

    private void ensureOneOfStatus(OutboundOrderEntity order, List<String> expected, String message) {
        if (!expected.contains(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
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

    private void ensureReviewerSeparation(Long createdBy) {
        if (createdBy == null || RequestContext.userId() == null) {
            return;
        }
        if (createdBy.equals(RequestContext.userId())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "创建人和审核人必须分离，请由管理员审核");
        }
    }

    private void ensureCurrentUserIsAdmin() {
        List<String> roleCodes = authQueryMapper.findRoleCodes(RequestContext.tenantId(), RequestContext.userId());
        boolean isAdmin = roleCodes != null && roleCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .anyMatch("TENANT_ADMIN"::equals);
        if (!isAdmin) {
            throw new BizException(ErrorCode.FORBIDDEN.code(), "仅管理员可以执行审核类操作");
        }
    }

    private void applyRemark(OutboundOrderEntity order, String note) {
        String trimmed = normalizeNote(note);
        if (StringUtils.hasText(trimmed)) {
            order.setRemark(trimmed);
        }
    }

    private String normalizeNote(String note) {
        if (!StringUtils.hasText(note)) return "";
        String cleaned = note.trim();
        return cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
    }

    private String defaultNote(String note, String fallback) {
        String cleaned = normalizeNote(note);
        return StringUtils.hasText(cleaned) ? cleaned : fallback;
    }

    private void recordAudit(OutboundOrderEntity order,
                             String action,
                             String beforeStatus,
                             String afterStatus,
                             String actionLabel,
                             String note) {
        String before = beforeStatus == null ? null : "{\"status\":\"" + beforeStatus + "\"}";
        String after = "{\"status\":\"" + afterStatus + "\",\"actionLabel\":\"" + safe(actionLabel) + "\",\"note\":\"" + safe(note) + "\",\"statusFrom\":\"" + safe(beforeStatus) + "\",\"statusTo\":\"" + safe(afterStatus) + "\"}";
        auditLogRecordService.record(MODULE, action, RESOURCE_TYPE, String.valueOf(order.getId()), order.getOutboundNo(), before, after);
    }

    private List<Map<String, Object>> loadTimeline(OutboundOrderEntity order) {
        List<AuditLogEntity> audits = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getTenantId, RequestContext.tenantId())
                .eq(AuditLogEntity::getResourceType, RESOURCE_TYPE)
                .eq(AuditLogEntity::getResourceId, String.valueOf(order.getId()))
                .orderByAsc(AuditLogEntity::getOccurredAt)
                .orderByAsc(AuditLogEntity::getId));

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (AuditLogEntity audit : audits) {
            String beforeStatus = readJsonText(audit.getBeforeJson(), "status");
            String statusFrom = readJsonText(audit.getAfterJson(), "statusFrom");
            String statusTo = readJsonText(audit.getAfterJson(), "statusTo");
            if (!StringUtils.hasText(statusFrom)) statusFrom = beforeStatus;
            if (!StringUtils.hasText(statusTo)) statusTo = readJsonText(audit.getAfterJson(), "status");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("occurredAt", audit.getOccurredAt());
            item.put("operatorId", audit.getOperatorId());
            item.put("operatorName", audit.getOperatorName());
            item.put("action", audit.getAction());
            item.put("actionLabel", resolveActionLabel(audit));
            item.put("statusFrom", StringUtils.hasText(statusFrom) ? statusFrom : "-");
            item.put("statusTo", StringUtils.hasText(statusTo) ? statusTo : "-");
            item.put("note", resolveNote(audit));
            timeline.add(item);
        }
        return timeline;
    }

    private String resolveActionLabel(AuditLogEntity audit) {
        String fromAfter = readJsonText(audit.getAfterJson(), "actionLabel");
        if (StringUtils.hasText(fromAfter)) return fromAfter;
        return switch (String.valueOf(audit.getAction()).toUpperCase(Locale.ROOT)) {
            case "CREATE" -> "创建出库单";
            case "UPDATE" -> "编辑出库单";
            case "SUBMIT" -> "提交出库单";
            case "WITHDRAW" -> "撤回出库单";
            case "APPROVE" -> "审核通过";
            case "REJECT" -> "驳回单据";
            case "UNAPPROVE" -> "反审核";
            case "CANCEL" -> "作废单据";
            case "SHIP" -> "发运出库";
            default -> audit.getAction();
        };
    }

    private String resolveNote(AuditLogEntity audit) {
        String note = readJsonText(audit.getAfterJson(), "note");
        if (StringUtils.hasText(note)) return note;
        return "-";
    }

    private String readJsonText(String raw, String key) {
        if (!StringUtils.hasText(raw)) return "";
        try {
            JsonNode node = objectMapper.readTree(raw);
            JsonNode value = node.get(key);
            return value == null || value.isNull() ? "" : value.asText("");
        } catch (Exception ignore) {
            return "";
        }
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "'");
    }

    private void syncSalesAfterShip(OutboundOrderEntity outbound, List<OutboundOrderItemEntity> outboundItems) {
        if (!"SALES_ORDER".equals(outbound.getSourceType()) || outbound.getSourceOrderId() == null) {
            return;
        }
        SalesOrderEntity sales = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrderEntity>()
                .eq(SalesOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(SalesOrderEntity::getId, outbound.getSourceOrderId()));
        if (sales == null) {
            return;
        }

        for (OutboundOrderItemEntity outboundItem : outboundItems) {
            if (outboundItem.getSourceOrderItemId() == null) {
                continue;
            }
            SalesOrderItemEntity salesItem = salesOrderItemMapper.selectOne(new LambdaQueryWrapper<SalesOrderItemEntity>()
                    .eq(SalesOrderItemEntity::getTenantId, RequestContext.tenantId())
                    .eq(SalesOrderItemEntity::getId, outboundItem.getSourceOrderItemId()));
            if (salesItem == null) {
                continue;
            }
            BigDecimal before = nullToZero(salesItem.getShippedQty());
            BigDecimal delta = nullToZero(outboundItem.getShippedQty());
            BigDecimal after = before.add(delta);
            if (after.compareTo(salesItem.getOrderQty()) > 0) {
                after = salesItem.getOrderQty();
            }
            salesItem.setShippedQty(after);
            salesItem.setUpdatedBy(RequestContext.userId());
            salesOrderItemMapper.updateById(salesItem);
        }

        String beforeStatus = sales.getStatus();
        List<SalesOrderItemEntity> salesItems = salesOrderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItemEntity>()
                .eq(SalesOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(SalesOrderItemEntity::getSalesOrderId, sales.getId()));
        boolean anyShipped = salesItems.stream().anyMatch(item -> nullToZero(item.getShippedQty()).compareTo(BigDecimal.ZERO) > 0);
        boolean allShipped = salesItems.stream().allMatch(item -> nullToZero(item.getShippedQty()).compareTo(item.getOrderQty()) >= 0);
        String afterStatus = allShipped ? "FULLY_SHIPPED" : (anyShipped ? "PARTIAL_SHIPPED" : "CONFIRMED");
        if (!afterStatus.equals(beforeStatus)) {
            sales.setStatus(afterStatus);
            sales.setUpdatedBy(RequestContext.userId());
            salesOrderMapper.updateById(sales);
        }

        String beforeJson = "{\"status\":\"" + beforeStatus + "\"}";
        String afterJson = "{\"status\":\"" + afterStatus + "\",\"outboundNo\":\"" + outbound.getOutboundNo()
                + "\",\"sourceOrderNo\":\"" + sales.getSalesNo() + "\"}";
        auditLogRecordService.record("ERP_SALES", "SHIP_STATUS_SYNC", "SALES_ORDER",
                String.valueOf(sales.getId()), sales.getSalesNo(), beforeJson, afterJson);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
