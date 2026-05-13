package com.novadepot.backend.modules.notifications;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.model.entity.NotificationEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationsService {
    private final NotificationMapper notificationMapper;
    private final AuditLogRecordService auditLogRecordService;

    public NotificationsService(NotificationMapper notificationMapper, AuditLogRecordService auditLogRecordService) {
        this.notificationMapper = notificationMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public Map<String, Object> list(Boolean unreadOnly, Integer pageNo, Integer pageSize) {
        int safePageNo = Math.max(1, pageNo == null ? 1 : pageNo);
        int safePageSize = Math.max(1, Math.min(100, pageSize == null ? 20 : pageSize));
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<NotificationEntity> base = baseQuery(unreadOnly);
        Long total = notificationMapper.selectCount(base);
        var rows = notificationMapper.selectList(baseQuery(unreadOnly)
                .orderByAsc(NotificationEntity::getReadFlag)
                .orderByDesc(NotificationEntity::getSentAt)
                .orderByDesc(NotificationEntity::getId)
                .last("limit " + offset + "," + safePageSize))
                .stream()
                .map(this::toView)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", rows);
        result.put("total", total == null ? 0 : total);
        result.put("pageNo", safePageNo);
        result.put("pageSize", safePageSize);
        return result;
    }

    public Map<String, Object> unreadCount() {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getTenantId, RequestContext.tenantId())
                .eq(NotificationEntity::getReceiverUserId, RequestContext.userId())
                .eq(NotificationEntity::getReadFlag, 0));
        return Map.of("unreadCount", count == null ? 0 : count);
    }

    public Map<String, Object> detail(Long id) {
        NotificationEntity entity = findMine(id);
        return toView(entity);
    }

    public Map<String, Object> markRead(Long id) {
        NotificationEntity entity = findMine(id);
        if (entity.getReadFlag() == null || entity.getReadFlag() == 0) {
            entity.setReadFlag(1);
            entity.setReadAt(LocalDateTime.now());
            entity.setUpdatedBy(RequestContext.userId());
            notificationMapper.updateById(entity);
            auditLogRecordService.record("NOTIFICATION", "MARK_READ", "NOTIFICATION", String.valueOf(id), entity.getBizNo(), null, "{\"readFlag\":1}");
        }
        return toView(entity);
    }

    public Map<String, Object> markAllRead() {
        Long unread = (Long) unreadCount().get("unreadCount");
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationEntity>()
                .eq(NotificationEntity::getTenantId, RequestContext.tenantId())
                .eq(NotificationEntity::getReceiverUserId, RequestContext.userId())
                .eq(NotificationEntity::getReadFlag, 0)
                .set(NotificationEntity::getReadFlag, 1)
                .set(NotificationEntity::getReadAt, LocalDateTime.now())
                .set(NotificationEntity::getUpdatedBy, RequestContext.userId()));
        auditLogRecordService.record("NOTIFICATION", "MARK_ALL_READ", "NOTIFICATION", String.valueOf(RequestContext.userId()), null, null, "{\"count\":" + unread + "}");
        return unreadCount();
    }

    public NotificationEntity createIfAbsent(Long receiverUserId,
                                             String notifyType,
                                             String bizType,
                                             String bizNo,
                                             String title,
                                             String content,
                                             String severity,
                                             String jumpPath) {
        NotificationEntity existing = notificationMapper.selectOne(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getTenantId, RequestContext.tenantId())
                .eq(NotificationEntity::getReceiverUserId, receiverUserId)
                .eq(NotificationEntity::getNotifyType, notifyType)
                .eq(StringUtils.hasText(bizType), NotificationEntity::getBizType, bizType)
                .eq(StringUtils.hasText(bizNo), NotificationEntity::getBizNo, bizNo)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setReceiverUserId(receiverUserId);
        entity.setNotifyType(limit(notifyType, 32));
        entity.setBizType(limit(bizType, 32));
        entity.setBizNo(limit(bizNo, 64));
        entity.setTitle(limit(title, 255));
        entity.setContent(content == null ? "" : content);
        entity.setSeverity(StringUtils.hasText(severity) ? limit(severity.toUpperCase(), 16) : "INFO");
        entity.setJumpPath(StringUtils.hasText(jumpPath) ? limit(jumpPath, 255) : resolveJumpPath(bizType, bizNo));
        entity.setReadFlag(0);
        entity.setSentAt(LocalDateTime.now());
        entity.setCreatedBy(RequestContext.userId());
        entity.setUpdatedBy(RequestContext.userId());
        notificationMapper.insert(entity);
        return entity;
    }

    private LambdaQueryWrapper<NotificationEntity> baseQuery(Boolean unreadOnly) {
        LambdaQueryWrapper<NotificationEntity> qw = new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getTenantId, RequestContext.tenantId())
                .eq(NotificationEntity::getReceiverUserId, RequestContext.userId());
        if (Boolean.TRUE.equals(unreadOnly)) {
            qw.eq(NotificationEntity::getReadFlag, 0);
        }
        return qw;
    }

    private NotificationEntity findMine(Long id) {
        NotificationEntity entity = notificationMapper.selectOne(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getTenantId, RequestContext.tenantId())
                .eq(NotificationEntity::getReceiverUserId, RequestContext.userId())
                .eq(NotificationEntity::getId, id)
                .last("limit 1"));
        if (entity == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "通知不存在或无权访问");
        }
        return entity;
    }

    private Map<String, Object> toView(NotificationEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(entity.getId()));
        result.put("notifyType", entity.getNotifyType());
        result.put("bizType", entity.getBizType());
        result.put("bizNo", entity.getBizNo());
        result.put("title", entity.getTitle());
        result.put("content", entity.getContent());
        result.put("severity", entity.getSeverity() == null ? "INFO" : entity.getSeverity());
        result.put("readFlag", entity.getReadFlag() == null ? 0 : entity.getReadFlag());
        result.put("sentAt", entity.getSentAt());
        result.put("readAt", entity.getReadAt());
        result.put("jumpPath", StringUtils.hasText(entity.getJumpPath()) ? entity.getJumpPath() : resolveJumpPath(entity.getBizType(), entity.getBizNo()));
        return result;
    }

    private String resolveJumpPath(String bizType, String bizNo) {
        String type = bizType == null ? "" : bizType.toUpperCase();
        return switch (type) {
            case "LOW_STOCK", "INVENTORY" -> "/wms/inventory?from=notification&focus=low-stock";
            case "INBOUND_ORDER" -> "/wms/inbound";
            case "OUTBOUND_ORDER" -> "/wms/outbound";
            case "PURCHASE_ORDER" -> "/erp/purchases";
            case "SALES_ORDER" -> "/erp/sales";
            case "CS_TICKET" -> "/cs/workspace";
            default -> StringUtils.hasText(bizNo) ? "/dashboard" : "/notifications";
        };
    }

    private String limit(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
