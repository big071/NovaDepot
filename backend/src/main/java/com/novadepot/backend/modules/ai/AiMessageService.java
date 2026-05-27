package com.novadepot.backend.modules.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AIConversationEntity;
import com.novadepot.backend.model.entity.AIMessageEntity;
import com.novadepot.backend.repository.AIConversationMapper;
import com.novadepot.backend.repository.AIMessageMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class AiMessageService {
    private final AIMessageMapper messageMapper;
    private final AIConversationMapper conversationMapper;

    public AiMessageService(AIMessageMapper messageMapper, AIConversationMapper conversationMapper) {
        this.messageMapper = messageMapper;
        this.conversationMapper = conversationMapper;
    }

    public List<Map<String, Object>> conversationMessages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<AIMessageEntity>()
                        .eq(AIMessageEntity::getTenantId, RequestContext.tenantId())
                        .eq(AIMessageEntity::getConversationId, conversationId)
                        .orderByAsc(AIMessageEntity::getId))
                .stream()
                .map(message -> Map.<String, Object>of(
                        "id", message.getId(),
                        "conversationId", message.getConversationId(),
                        "role", message.getRole(),
                        "content", message.getContent(),
                        "tokens", message.getTokens() == null ? 0 : message.getTokens(),
                        "latencyMs", message.getLatencyMs() == null ? 0 : message.getLatencyMs(),
                        "confidence", message.getConfidence() == null ? BigDecimal.ZERO : message.getConfidence(),
                        "errorCode", message.getErrorCode() == null ? "" : message.getErrorCode(),
                        "status", message.getStatus() == null ? "COMPLETED" : message.getStatus(),
                        "createdAt", message.getCreatedAt()
                )).toList();
    }

    public List<Map<String, Object>> conversationMessagesByNo(String conversationNo) {
        AIConversationEntity conversation = conversationMapper.selectOne(new LambdaQueryWrapper<AIConversationEntity>()
                .eq(AIConversationEntity::getTenantId, RequestContext.tenantId())
                .eq(AIConversationEntity::getConversationNo, conversationNo)
                .last("limit 1"));
        if (conversation == null || conversation.getId() == null) {
            return List.of();
        }
        return conversationMessages(conversation.getId());
    }

    public AIMessageEntity saveMessage(Long conversationId,
                                       String role,
                                       String content,
                                       BigDecimal confidence,
                                       Integer tokens,
                                       Integer latencyMs,
                                       String errorCode,
                                       String status) {
        AIMessageEntity message = new AIMessageEntity();
        message.setTenantId(RequestContext.tenantId());
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setConfidence(confidence);
        message.setTokens(tokens);
        message.setLatencyMs(latencyMs);
        message.setErrorCode(errorCode);
        message.setStatus(status);
        message.setCreatedBy(RequestContext.userId());
        message.setUpdatedBy(RequestContext.userId());
        messageMapper.insert(message);
        return message;
    }

    public void updateMessage(AIMessageEntity message,
                              String content,
                              BigDecimal confidence,
                              Integer tokens,
                              Integer latencyMs,
                              String errorCode,
                              String status) {
        message.setContent(content == null ? "" : content);
        message.setConfidence(confidence);
        message.setTokens(tokens);
        message.setLatencyMs(latencyMs);
        message.setErrorCode(errorCode);
        message.setStatus(status);
        message.setUpdatedBy(RequestContext.userId());
        messageMapper.updateById(message);
    }

    public List<Map<String, String>> recentContextMessages(Long conversationId) {
        List<AIMessageEntity> rows = messageMapper.selectList(new LambdaQueryWrapper<AIMessageEntity>()
                .eq(AIMessageEntity::getTenantId, RequestContext.tenantId())
                .eq(AIMessageEntity::getConversationId, conversationId)
                .in(AIMessageEntity::getRole, List.of("USER", "ASSISTANT"))
                .in(AIMessageEntity::getStatus, List.of("COMPLETED", "STOPPED"))
                .orderByDesc(AIMessageEntity::getId)
                .last("limit 40"));
        java.util.Collections.reverse(rows);
        return rows.stream()
                .map(message -> Map.of(
                        "role", "ASSISTANT".equalsIgnoreCase(message.getRole()) ? "assistant" : "user",
                        "content", message.getContent() == null ? "" : message.getContent()
                ))
                .toList();
    }
}
