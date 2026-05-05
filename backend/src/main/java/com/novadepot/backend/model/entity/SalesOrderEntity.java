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
@TableName("sales_orders")
public class SalesOrderEntity extends BaseEntity {
    private String salesNo;
    private String status;
    private Long partnerId;
    private Long customerId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private String remark;
}
