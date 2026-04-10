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
@TableName("inventory")
public class InventoryEntity extends BaseEntity {
    private Long warehouseId;
    private Long locationId;
    private Long productId;
    private BigDecimal availableQty;
    private BigDecimal lockedQty;
    private BigDecimal inTransitQty;
    private Integer versionNo;
}
