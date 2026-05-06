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
@TableName("inbound_orders")
public class InboundOrderEntity extends BaseEntity {
    private String inboundNo;
    private String bizType;
    private String status;
    private String sourceType;
    private Long sourceOrderId;
    private String sourceOrderNo;
    private Long warehouseId;
    private Long supplierId;
    private LocalDateTime expectedAt;
    private LocalDateTime completedAt;
    private String remark;
}
