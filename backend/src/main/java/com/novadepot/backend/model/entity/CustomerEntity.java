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
@TableName("customers")
public class CustomerEntity extends BaseEntity {
    private String customerCode;
    private String customerName;
    private String contactName;
    private String phone;
    private String customerLevel;
    private String status;
}
