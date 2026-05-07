package com.novadepot.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novadepot.backend.common.api.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@TableName("payments")
public class PaymentEntity extends BaseEntity {
    private String paymentNo;
    private String direction;
    private Long ledgerId;
    private String ledgerNo;
    private Long partnerId;
    private BigDecimal amount;
    private LocalDate paidAt;
    private String method;
    private String remark;
}
