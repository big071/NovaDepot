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
@TableName("warehouses")
public class WarehouseEntity extends BaseEntity {
    private String warehouseCode;
    private String warehouseName;
    private String warehouseType;
    private String address;
    private Long managerUserId;
    private String status;
}
