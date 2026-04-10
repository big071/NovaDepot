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
@TableName("inventory_transactions")
public class InventoryTransactionEntity extends BaseEntity {
    private String txnNo;
    private String bizType;
    private String bizNo;
    private Long warehouseId;
    private Long locationId;
    private Long productId;
    private BigDecimal changeQty;
    private BigDecimal beforeQty;
    private BigDecimal afterQty;
    private String requestId;
    private Long operatorId;
    private LocalDateTime occurredAt;
}
