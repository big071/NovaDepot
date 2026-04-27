package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("business_history_events")
public class BusinessHistoryEventEntity extends BaseEntity {
    private String resourceType;
    private String resourceId;
    private String bizNo;
    private String action;
    private String actionLabel;
    private String statusFrom;
    private String statusTo;
    private String note;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime occurredAt;
}

