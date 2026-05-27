package com.novadepot.backend.modules.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.AIConversationEntity;
import com.novadepot.backend.repository.AIConversationMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiConversationService {
    private final AIConversationMapper conversationMapper;
    private final AiAuditService auditService;
    private final AiProviderResolver providerResolver;
    private final AiPromptService promptService;

    public AiConversationService(AIConversationMapper conversationMapper,
                                 AiAuditService auditService,
                                 AiProviderResolver providerResolver,
                                 AiPromptService promptService) {
        this.conversationMapper = conversationMapper;
        this.auditService = auditService;
        this.providerResolver = providerResolver;
        this.promptService = promptService;
    }

    public List<Map<String, Object>> conversations() {
        List<AIConversationEntity> list = conversationMapper.selectList(new LambdaQueryWrapper<AIConversationEntity>()
                .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(AIConversationEntity::getStartedAt)
                .last("limit 50"));

        return list.stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "conversationNo", c.getConversationNo(),
                "scene", c.getScene(),
                "provider", c.getProviderType(),
                "status", c.getStatus(),
                "startedAt", c.getStartedAt(),
                "lastActiveAt", c.getLastActiveAt() == null ? c.getStartedAt() : c.getLastActiveAt()
        )).toList();
    }

    public Map<String, Object> createConversation(String scene) {
        String normalizedScene = promptService.normalizeScene(scene);
        String providerName = providerResolver.resolveProviderName(null);
        AIConversationEntity created = createNewConversation(normalizedScene, providerName);
        auditService.writeAudit("AI_CONVERSATION_CREATE", created.getId(), created.getConversationNo(), null,
                Map.of("scene", normalizedScene, "status", created.getStatus()));
        return conversationToMap(created);
    }

    public Map<String, Object> archiveConversation(Long conversationId) {
        AIConversationEntity conversation = conversationMapper.selectOne(new LambdaQueryWrapper<AIConversationEntity>()
                .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                .eq(AIConversationEntity::getId, conversationId));
        if (conversation == null) {
            return Map.of("archived", false, "reason", "NOT_FOUND");
        }
        Map<String, Object> before = conversationToMap(conversation);
        conversation.setStatus("ARCHIVED");
        conversation.setEndedAt(LocalDateTime.now());
        conversation.setLastActiveAt(conversation.getLastActiveAt() == null ? LocalDateTime.now() : conversation.getLastActiveAt());
        conversation.setUpdatedBy(RequestContext.userId());
        conversationMapper.updateById(conversation);
        auditService.writeAudit("AI_CONVERSATION_ARCHIVE", conversation.getId(), conversation.getConversationNo(), before, conversationToMap(conversation));
        return conversationToMap(conversation);
    }

    public AIConversationEntity resolveConversation(Long conversationId, String scene, String providerName) {
        if (conversationId != null) {
            AIConversationEntity existed = conversationMapper.selectOne(new LambdaQueryWrapper<AIConversationEntity>()
                    .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                    .eq(AIConversationEntity::getId, conversationId));
            if (existed != null) {
                return existed;
            }
        }

        return createNewConversation(scene, providerName);
    }

    public AIConversationEntity resolveConversation(Long conversationId, String conversationNo, String scene, String providerName) {
        if (conversationId != null) {
            AIConversationEntity existed = conversationMapper.selectOne(new LambdaQueryWrapper<AIConversationEntity>()
                    .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                    .eq(AIConversationEntity::getId, conversationId));
            if (existed != null) {
                return existed;
            }
        }
        if (StringUtils.hasText(conversationNo)) {
            AIConversationEntity existed = conversationMapper.selectOne(new LambdaQueryWrapper<AIConversationEntity>()
                    .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                    .eq(AIConversationEntity::getConversationNo, conversationNo.trim())
                    .last("limit 1"));
            if (existed != null) {
                return existed;
            }
        }
        return createNewConversation(scene, providerName);
    }

    public void ensureWritable(AIConversationEntity conversation) {
        if ("ARCHIVED".equalsIgnoreCase(conversation.getStatus())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "归档会话只读，请新建会话后继续提问。");
        }
    }

    public AIConversationEntity createNewConversation(String scene, String providerName) {
        AIConversationEntity created = new AIConversationEntity();
        created.setTenantId(RequestContext.tenantId());
        created.setConversationNo(NoGenerator.next("AI"));
        created.setScene(scene);
        created.setProviderType(providerName);
        created.setStatus("ACTIVE");
        created.setStartedAt(LocalDateTime.now());
        created.setLastActiveAt(LocalDateTime.now());
        created.setCreatedBy(RequestContext.userId());
        created.setUpdatedBy(RequestContext.userId());
        conversationMapper.insert(created);
        return created;
    }

    public void touchConversation(AIConversationEntity conversation, String providerName, Object model) {
        conversation.setProviderType(providerName);
        if (model != null) {
            conversation.setModelName(String.valueOf(model));
        }
        conversation.setLastActiveAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    public Map<String, Object> conversationToMap(AIConversationEntity c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("conversationNo", c.getConversationNo());
        map.put("scene", c.getScene());
        map.put("provider", c.getProviderType());
        map.put("status", c.getStatus());
        map.put("startedAt", c.getStartedAt());
        map.put("lastActiveAt", c.getLastActiveAt() == null ? c.getStartedAt() : c.getLastActiveAt());
        map.put("endedAt", c.getEndedAt());
        return map;
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void archiveInactiveConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<AIConversationEntity> rows = conversationMapper.selectList(new LambdaQueryWrapper<AIConversationEntity>()
                .eq(AIConversationEntity::getStatus, "ACTIVE")
                .lt(AIConversationEntity::getLastActiveAt, cutoff)
                .last("limit 200"));
        for (AIConversationEntity row : rows) {
            row.setStatus("ARCHIVED");
            row.setEndedAt(LocalDateTime.now());
            conversationMapper.updateById(row);
        }
    }
}
