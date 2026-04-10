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
@TableName("sales_order_items")
public class SalesOrderItemEntity extends BaseEntity {
    private Long salesOrderId;
    private Integer lineNo;
    private Long productId;
    private BigDecimal unitPrice;
    private BigDecimal orderQty;
    private BigDecimal shippedQty;
    private BigDecimal taxRate;
}
