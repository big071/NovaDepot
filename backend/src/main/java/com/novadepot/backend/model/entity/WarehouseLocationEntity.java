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
@TableName("warehouse_locations")
public class WarehouseLocationEntity extends BaseEntity {
    private Long warehouseId;
    private String locationCode;
    private String locationName;
    private String locationType;
    private BigDecimal capacityQty;
    private String status;
}
