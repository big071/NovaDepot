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
@TableName("ai_messages")
public class AIMessageEntity extends BaseEntity {
    private Long conversationId;
    private String role;
    private String content;
    private Integer tokens;
    private Integer latencyMs;
    private BigDecimal confidence;
    private String errorCode;
}
