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
@TableName("product_units")
public class ProductUnitEntity extends BaseEntity {
    private String unitCode;
    private String unitName;
    private Integer precisionScale;
    private String status;
}
