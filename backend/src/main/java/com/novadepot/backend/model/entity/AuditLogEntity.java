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
@TableName("audit_logs")
public class AuditLogEntity extends BaseEntity {
    private String module;
    private String action;
    private String resourceType;
    private String resourceId;
    private String bizNo;
    private Long operatorId;
    private String operatorName;
    private String beforeJson;
    private String afterJson;
    private String ip;
    private String userAgent;
    private LocalDateTime occurredAt;
}
