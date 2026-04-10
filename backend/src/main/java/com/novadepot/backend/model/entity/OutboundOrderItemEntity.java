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
@TableName("outbound_order_items")
public class OutboundOrderItemEntity extends BaseEntity {
    private Long outboundOrderId;
    private Integer lineNo;
    private Long productId;
    private Long locationId;
    private Long unitId;
    private String batchNo;
    private BigDecimal planQty;
    private BigDecimal pickedQty;
    private BigDecimal shippedQty;
}
