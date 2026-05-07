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
@TableName("outbound_orders")
public class OutboundOrderEntity extends BaseEntity {
    private String outboundNo;
    private String bizType;
    private String status;
    private String sourceType;
    private Long sourceOrderId;
    private String sourceOrderNo;
    private Long warehouseId;
    private Long customerId;
    private LocalDateTime expectedShipAt;
    private LocalDateTime shippedAt;
    private String remark;
}
