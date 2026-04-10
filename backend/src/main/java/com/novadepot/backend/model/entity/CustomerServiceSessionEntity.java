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
@TableName("customer_service_sessions")
public class CustomerServiceSessionEntity extends BaseEntity {
    private String sessionNo;
    private String channel;
    private Long customerId;
    private String status;
    private Long assignedUserId;
    private String priority;
    private LocalDateTime firstResponseAt;
    private LocalDateTime closedAt;
}
