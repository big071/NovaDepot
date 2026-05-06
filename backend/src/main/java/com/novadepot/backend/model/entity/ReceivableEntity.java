package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("receivables")
public class ReceivableEntity extends BaseEntity {
    private String receivableNo;
    private String sourceType;
    private Long sourceOrderId;
    private String sourceOrderNo;
    private Long partnerId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal balanceAmount;
    private String status;
    private String remark;
}
