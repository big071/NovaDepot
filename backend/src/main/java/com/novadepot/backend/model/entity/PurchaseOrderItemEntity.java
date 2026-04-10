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
@TableName("purchase_order_items")
public class PurchaseOrderItemEntity extends BaseEntity {
    private Long purchaseOrderId;
    private Integer lineNo;
    private Long productId;
    private BigDecimal unitPrice;
    private BigDecimal orderQty;
    private BigDecimal receivedQty;
    private BigDecimal taxRate;
}
