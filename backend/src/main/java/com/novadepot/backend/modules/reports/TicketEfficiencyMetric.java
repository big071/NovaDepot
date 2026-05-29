package com.novadepot.backend.modules.reports;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketEfficiencyMetric {
    private Long assigneeId;
    private Long ticketCount;
    private Long closedCount;
}
