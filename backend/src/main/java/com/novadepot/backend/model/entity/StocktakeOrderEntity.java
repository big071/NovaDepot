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
@TableName("stocktake_orders")
public class StocktakeOrderEntity extends BaseEntity {
    private String stocktakeNo;
    private String status;
    private Long warehouseId;
    private String scopeType;
    private LocalDateTime plannedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer diffCount;
    private String remark;
}
