package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("customer_service_tickets")
public class CustomerServiceTicketEntity extends BaseEntity {
    private String ticketNo;
    private Long sessionId;
    private String priority;
    private String content;
    private String status;
    private Long assigneeUserId;
    private String remark;
}
