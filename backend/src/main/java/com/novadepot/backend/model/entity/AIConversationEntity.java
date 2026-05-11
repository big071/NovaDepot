package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_conversations")
public class AIConversationEntity extends BaseEntity {
    private String conversationNo;
    private String scene;
    private String bizType;
    private String bizNo;
    private String providerType;
    private String modelName;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime lastActiveAt;
}
