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
@TableName("notifications")
public class NotificationEntity extends BaseEntity {
    private String notifyType;
    private String bizType;
    private String bizNo;
    private Long receiverUserId;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
