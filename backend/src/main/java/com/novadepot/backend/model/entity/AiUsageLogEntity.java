package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("ai_usage_logs")
public class AiUsageLogEntity extends BaseEntity {
    private Long conversationId;
    private String provider;
    private String model;
    private String scene;
    private String role;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private Integer success;
    private String errorCode;
    private String errorMessage;
    private BigDecimal costEstimate;
}