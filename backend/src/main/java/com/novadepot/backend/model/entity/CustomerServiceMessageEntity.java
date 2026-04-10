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
@TableName("customer_service_messages")
public class CustomerServiceMessageEntity extends BaseEntity {
    private Long sessionId;
    private String senderType;
    private Long senderId;
    private String content;
    private String msgType;
    private Integer aiSuggested;
}
