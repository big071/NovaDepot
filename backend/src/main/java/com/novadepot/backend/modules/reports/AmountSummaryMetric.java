package com.novadepot.backend.modules.reports;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AmountSummaryMetric {
    private Long count;
    private BigDecimal amount;
}
