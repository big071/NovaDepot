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
@TableName("permissions")
public class PermissionEntity extends BaseEntity {
    private String permCode;
    private String permName;
    private String resource;
    private String action;
    private String status;
}
