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
@TableName("purchase_orders")
public class PurchaseOrderEntity extends BaseEntity {
    private String purchaseNo;
    private String status;
    private Long partnerId;
    private Long supplierId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    private LocalDate expectedArrivalDate;
    private String remark;
}
