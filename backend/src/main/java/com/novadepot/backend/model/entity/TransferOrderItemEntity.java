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
@TableName("transfer_order_items")
public class TransferOrderItemEntity extends BaseEntity {
    private Long transferOrderId;
    private Integer lineNo;
    private Long productId;
    private Long unitId;
    private String batchNo;
    private BigDecimal planQty;
    private BigDecimal actualQty;
}
