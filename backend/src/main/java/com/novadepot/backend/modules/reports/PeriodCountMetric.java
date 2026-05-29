package com.novadepot.backend.modules.reports;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodCountMetric {
    private String period;
    private Long count;
}
