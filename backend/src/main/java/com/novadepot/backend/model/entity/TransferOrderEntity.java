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
@TableName("transfer_orders")
public class TransferOrderEntity extends BaseEntity {
    private String transferNo;
    private String status;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private Long fromLocationId;
    private Long toLocationId;
    private LocalDateTime completedAt;
    private String remark;
}
