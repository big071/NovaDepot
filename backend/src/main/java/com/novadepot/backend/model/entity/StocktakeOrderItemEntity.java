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
@TableName("stocktake_order_items")
public class StocktakeOrderItemEntity extends BaseEntity {
    private Long stocktakeOrderId;
    private Integer lineNo;
    private Long productId;
    private Long locationId;
    private BigDecimal systemQty;
    private BigDecimal countedQty;
    private BigDecimal diffQty;
    private String resultType;
}
