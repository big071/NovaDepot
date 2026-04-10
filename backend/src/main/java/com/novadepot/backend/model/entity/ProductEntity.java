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
@TableName("products")
public class ProductEntity extends BaseEntity {
    private String productCode;
    private String productName;
    private Long categoryId;
    private Long unitId;
    private String barcode;
    private String spec;
    private Integer batchEnabled;
    private Integer shelfLifeDays;
    private String status;
}
