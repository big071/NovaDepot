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
@TableName("product_categories")
public class ProductCategoryEntity extends BaseEntity {
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer sortNo;
    private String status;
}
