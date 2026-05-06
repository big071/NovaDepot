package com.novadepot.backend.modules.inboundorders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.AuditLogEntity;
import com.novadepot.backend.model.entity.InboundOrderEntity;
import com.novadepot.backend.model.entity.InboundOrderItemEntity;
import com.novadepot.backend.model.entity.InventoryEntity;
import com.novadepot.backend.model.entity.InventoryTransactionEntity;
import com.novadepot.backend.model.entity.PurchaseOrderEntity;
import com.novadepot.backend.model.entity.PurchaseOrderItemEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.AuditLogMapper;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.InboundOrderItemMapper;
import com.novadepot.backend.repository.InboundOrderMapper;
import com.novadepot.backend.repository.InventoryMapper;
import com.novadepot.backend.repository.InventoryTransactionMapper;
import com.novadepot.backend.repository.PurchaseOrderItemMapper;
import com.novadepot.backend.repository.PurchaseOrderMapper;
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
public class InboundOrdersService {
    private static final String MODULE = "WMS_INBOUND";
    private static final String RESOURCE_TYPE = "INBOUND_ORDER";

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final AuditLogMapper auditLogMapper;
    private final AuthQueryMapper authQueryMapper;
    private final ObjectMapper objectMapper;

    public InboundOrdersService(InboundOrderMapper inboundOrderMapper,
                                InboundOrderItemMapper inboundOrderItemMapper,
                                InventoryMapper inventoryMapper,
                                InventoryTransactionMapper inventoryTransactionMapper,
                                PurchaseOrderMapper purchaseOrderMapper,
                                PurchaseOrderItemMapper purchaseOrderItemMapper,
                                AuditLogRecordService auditLogRecordService,
                                AuditLogMapper auditLogMapper,
                                AuthQueryMapper authQueryMapper,
                                ObjectMapper objectMapper) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryTransactionMapper = inventoryTransactionMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.auditLogRecordService = auditLogRecordService;
        this.auditLogMapper = auditLogMapper;
        this.authQueryMapper = authQueryMapper;
        this.objectMapper = objectMapper;
    }

    public List<InboundOrderEntity> list() {
        return inboundOrderMapper.selectList(new LambdaQueryWrapper<InboundOrderEntity>()
                .eq(InboundOrderEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(InboundOrderEntity::getId));
    }

    public List<Map<String, Object>> items(Long id) {
        mustGet(id);
        List<InboundOrderItemEntity> rows = inboundOrderItemMapper.selectList(new LambdaQueryWrapper<InboundOrderItemEntity>()
                .eq(InboundOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(InboundOrderItemEntity::getInboundOrderId, id)
                .orderByAsc(InboundOrderItemEntity::getLineNo));
        return rows.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId() == null ? null : String.valueOf(item.getId()));
            row.put("lineNo", item.getLineNo());
            row.put("productId", item.getProductId() == null ? null : String.valueOf(item.getProductId()));
            row.put("locationId", item.getLocationId() == null ? null : String.valueOf(item.getLocationId()));
            row.put("sourceOrderItemId", item.getSourceOrderItemId() == null ? null : String.valueOf(item.getSourceOrderItemId()));
            row.put("sourceLineNo", item.getSourceLineNo());
            row.put("planQty", item.getPlanQty());
            row.put("receivedQty", item.getReceivedQty());
            row.put("qualifiedQty", item.getQualifiedQty());
            return row;
        }).toList();
    }

    public Map<String, Object> detail(Long id) {
        InboundOrderEntity order = mustGet(id);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("order", order);
        detail.put("items", items(id));
        detail.put("timeline", loadTimeline(order));
        detail.put("auditQuery", Map.of(
                "bizNo", order.getInboundNo(),
                "resourceType", RESOURCE_TYPE,
                "resourceId", String.valueOf(order.getId())
        ));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(InboundCreateRequest req) {
        InboundOrderEntity order = new InboundOrderEntity();
        order.setTenantId(RequestContext.tenantId());
        order.setInboundNo(NoGenerator.next("IN"));
        order.setBizType("PURCHASE");
        order.setStatus("DRAFT");
        order.setSourceType("MANUAL");
        order.setWarehouseId(req.getWarehouseId());
        order.setSupplierId(req.getSupplierId());
        order.setCreatedBy(RequestContext.userId());
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.insert(order);
        rewriteItems(order, req);
        recordAudit(order, "CREATE", null, "DRAFT", "创建入库单", "创建草稿");
        return Map.of("id", String.valueOf(order.getId()), "inboundNo", order.getInboundNo(), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, InboundCreateRequest req) {
        InboundOrderEntity order = mustGet(id);
        ensureOneOfStatus(order, List.of("DRAFT", "REJECTED"), "仅草稿或驳回状态可编辑");
        String before = order.getStatus();
        order.setWarehouseId(req.getWarehouseId());
        order.setSupplierId(req.getSupplierId());
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        rewriteItems(order, req);
        recordAudit(order, "UPDATE", before, before, "编辑入库单", "更新明细与基础信息");
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(Long id, String note) {
        InboundOrderEntity order = mustGet(id);
        ensureOneOfStatus(order, List.of("DRAFT", "REJECTED"), "仅草稿或驳回状态可提交");
        String before = order.getStatus();
        order.setStatus("SUBMITTED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        recordAudit(order, "SUBMIT", before, "SUBMITTED", "提交入库单", defaultNote(note, "提交审核"));
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> withdraw(Long id, String note) {
        InboundOrderEntity order = mustGet(id);
        ensureStatus(order, "SUBMITTED", "仅已提交状态可撤回");
        order.setStatus("DRAFT");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        recordAudit(order, "WITHDRAW", "SUBMITTED", "DRAFT", "撤回入库单", defaultNote(note, "提交后撤回"));
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(Long id, String note) {
        InboundOrderEntity order = mustGet(id);
        ensureOneOfStatus(order, List.of("DRAFT", "SUBMITTED", "REJECTED"), "仅草稿/已提交/驳回状态可作废");
        String before = order.getStatus();
        order.setStatus("CANCELED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        recordAudit(order, "CANCEL", before, "CANCELED", "作废入库单", defaultNote(note, "作废单据"));
        return Map.of("id", String.valueOf(order.getId()), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long id, String note) {
        InboundOrderEntity order = mustGet(id);
        ensureStatus(order, "SUBMITTED", "仅已提交状态可审核");
        ensureCurrentUserIsAdmin();
        ensureReviewerSeparation(order.getCreatedBy());
        order.setStatus("APPROVED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        recordAudit(order, "APPROVE", "SUBMITTED", "APPROVED", "审核通过", defaultNote(note, "审核通过"));
        return Map.of("id", String.valueOf(id), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(Long id, String note) {
        InboundOrderEntity order = mustGet(id);
        ensureStatus(order, "SUBMITTED", "仅已提交状态可驳回");
        ensureCurrentUserIsAdmin();
        ensureReviewerSeparation(order.getCreatedBy());
        order.setStatus("REJECTED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        recordAudit(order, "REJECT", "SUBMITTED", "REJECTED", "驳回单据", defaultNote(note, "驳回，请补充信息后重提"));
        return Map.of("id", String.valueOf(id), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> unapprove(Long id, String note) {
        InboundOrderEntity order = mustGet(id);
        ensureStatus(order, "APPROVED", "仅审核通过状态可反审核");
        ensureCurrentUserIsAdmin();
        order.setStatus("SUBMITTED");
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        recordAudit(order, "UNAPPROVE", "APPROVED", "SUBMITTED", "反审核", defaultNote(note, "反审核，回到待审核状态"));
        return Map.of("id", String.valueOf(id), "status", order.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> post(Long id, String note) {
        InboundOrderEntity order = mustGet(id);
        ensureStatus(order, "APPROVED", "仅审核通过状态可过账");

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

        order.setStatus("POSTED");
        order.setCompletedAt(LocalDateTime.now());
        applyRemark(order, note);
        order.setUpdatedBy(RequestContext.userId());
        inboundOrderMapper.updateById(order);
        recordAudit(order, "POST", "APPROVED", "POSTED", "过账入库", defaultNote(note, "完成入库过账"));
        syncPurchaseAfterPost(order, items);

        return Map.of("id", String.valueOf(id), "status", order.getStatus(), "inventoryPosted", true);
    }

    private void rewriteItems(InboundOrderEntity order, InboundCreateRequest req) {
        inboundOrderItemMapper.delete(new LambdaQueryWrapper<InboundOrderItemEntity>()
                .eq(InboundOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(InboundOrderItemEntity::getInboundOrderId, order.getId()));

        int lineNo = 1;
        for (InboundCreateRequest.Item item : req.getItems()) {
            InboundOrderItemEntity entity = new InboundOrderItemEntity();
            entity.setTenantId(RequestContext.tenantId());
            entity.setInboundOrderId(order.getId());
            entity.setLineNo(lineNo++);
            entity.setSourceOrderItemId(null);
            entity.setSourceLineNo(null);
            entity.setProductId(item.getProductId());
            entity.setLocationId(item.getLocationId());
            entity.setPlanQty(item.getQty());
            entity.setReceivedQty(BigDecimal.ZERO);
            entity.setQualifiedQty(BigDecimal.ZERO);
            entity.setUnitId(1L);
            inboundOrderItemMapper.insert(entity);
        }
    }

    private void ensureStatus(InboundOrderEntity order, String expected, String message) {
        if (!expected.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
    }

    private void ensureOneOfStatus(InboundOrderEntity order, List<String> expected, String message) {
        if (!expected.contains(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), message);
        }
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

    private void applyRemark(InboundOrderEntity order, String note) {
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

    private void recordAudit(InboundOrderEntity order,
                             String action,
                             String beforeStatus,
                             String afterStatus,
                             String actionLabel,
                             String note) {
        String before = beforeStatus == null ? null : "{\"status\":\"" + beforeStatus + "\"}";
        String after = "{\"status\":\"" + afterStatus + "\",\"actionLabel\":\"" + safe(actionLabel) + "\",\"note\":\"" + safe(note) + "\",\"statusFrom\":\"" + safe(beforeStatus) + "\",\"statusTo\":\"" + safe(afterStatus) + "\"}";
        auditLogRecordService.record(MODULE, action, RESOURCE_TYPE, String.valueOf(order.getId()), order.getInboundNo(), before, after);
    }

    private List<Map<String, Object>> loadTimeline(InboundOrderEntity order) {
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
            case "CREATE" -> "创建入库单";
            case "UPDATE" -> "编辑入库单";
            case "SUBMIT" -> "提交入库单";
            case "WITHDRAW" -> "撤回入库单";
            case "APPROVE" -> "审核通过";
            case "REJECT" -> "驳回单据";
            case "UNAPPROVE" -> "反审核";
            case "CANCEL" -> "作废单据";
            case "POST" -> "过账入库";
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

    private void syncPurchaseAfterPost(InboundOrderEntity inbound, List<InboundOrderItemEntity> inboundItems) {
        if (!"PURCHASE_ORDER".equals(inbound.getSourceType()) || inbound.getSourceOrderId() == null) {
            return;
        }
        PurchaseOrderEntity purchase = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderEntity>()
                .eq(PurchaseOrderEntity::getTenantId, RequestContext.tenantId())
                .eq(PurchaseOrderEntity::getId, inbound.getSourceOrderId()));
        if (purchase == null) {
            return;
        }

        for (InboundOrderItemEntity inboundItem : inboundItems) {
            if (inboundItem.getSourceOrderItemId() == null) {
                continue;
            }
            PurchaseOrderItemEntity purchaseItem = purchaseOrderItemMapper.selectOne(new LambdaQueryWrapper<PurchaseOrderItemEntity>()
                    .eq(PurchaseOrderItemEntity::getTenantId, RequestContext.tenantId())
                    .eq(PurchaseOrderItemEntity::getId, inboundItem.getSourceOrderItemId()));
            if (purchaseItem == null) {
                continue;
            }
            BigDecimal before = nullToZero(purchaseItem.getReceivedQty());
            BigDecimal delta = nullToZero(inboundItem.getQualifiedQty());
            BigDecimal after = before.add(delta);
            if (after.compareTo(purchaseItem.getOrderQty()) > 0) {
                after = purchaseItem.getOrderQty();
            }
            purchaseItem.setReceivedQty(after);
            purchaseItem.setUpdatedBy(RequestContext.userId());
            purchaseOrderItemMapper.updateById(purchaseItem);
        }

        String beforeStatus = purchase.getStatus();
        List<PurchaseOrderItemEntity> purchaseItems = purchaseOrderItemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItemEntity>()
                .eq(PurchaseOrderItemEntity::getTenantId, RequestContext.tenantId())
                .eq(PurchaseOrderItemEntity::getPurchaseOrderId, purchase.getId()));
        boolean anyReceived = purchaseItems.stream().anyMatch(item -> nullToZero(item.getReceivedQty()).compareTo(BigDecimal.ZERO) > 0);
        boolean allReceived = purchaseItems.stream().allMatch(item -> nullToZero(item.getReceivedQty()).compareTo(item.getOrderQty()) >= 0);
        String afterStatus = allReceived ? "FULLY_RECEIVED" : (anyReceived ? "PARTIAL_RECEIVED" : "CONFIRMED");
        if (!afterStatus.equals(beforeStatus)) {
            purchase.setStatus(afterStatus);
            purchase.setUpdatedBy(RequestContext.userId());
            purchaseOrderMapper.updateById(purchase);
        }

        String beforeJson = "{\"status\":\"" + beforeStatus + "\"}";
        String afterJson = "{\"status\":\"" + afterStatus + "\",\"inboundNo\":\"" + inbound.getInboundNo()
                + "\",\"sourceOrderNo\":\"" + purchase.getPurchaseNo() + "\"}";
        auditLogRecordService.record("ERP_PURCHASE", "RECEIVE_STATUS_SYNC", "PURCHASE_ORDER",
                String.valueOf(purchase.getId()), purchase.getPurchaseNo(), beforeJson, afterJson);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
