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
@TableName("suppliers")
public class SupplierEntity extends BaseEntity {
    private String supplierCode;
    private String supplierName;
    private String contactName;
    private String phone;
    private String creditLevel;
    private String status;
}
