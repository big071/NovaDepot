package com.novadepot.backend.modules.reports;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InventoryTurnoverMetric {
    private Long productId;
    private BigDecimal outboundQty;
    private BigDecimal availableQty;
}
