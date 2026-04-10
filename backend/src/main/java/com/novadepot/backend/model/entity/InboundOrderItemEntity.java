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
@TableName("inbound_order_items")
public class InboundOrderItemEntity extends BaseEntity {
    private Long inboundOrderId;
    private Integer lineNo;
    private Long productId;
    private Long locationId;
    private Long unitId;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expireDate;
    private BigDecimal planQty;
    private BigDecimal receivedQty;
    private BigDecimal qualifiedQty;
}
