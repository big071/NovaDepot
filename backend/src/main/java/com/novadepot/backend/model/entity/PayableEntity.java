package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("payables")
public class PayableEntity extends BaseEntity {
    private String payableNo;
    private String sourceType;
    private Long sourceOrderId;
    private String sourceOrderNo;
    private Long partnerId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String status;
    private String remark;
}
