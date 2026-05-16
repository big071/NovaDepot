package com.novadepot.backend.modules.agent.patrol;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentPatrolScheduler {
    private final AgentPatrolService service;

    @Value("${app.agent.patrol.enabled:true}")
    private boolean enabled;
    @Value("${app.agent.patrol.order-overdue-hours:24}")
    private int orderOverdueHours;
    @Value("${app.agent.patrol.ticket-overdue-hours:4}")
    private int ticketOverdueHours;

    public AgentPatrolScheduler(AgentPatrolService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.agent.patrol.low-stock-cron:0 0 * * * ?}")
    public void lowStockPatrol() {
        if (enabled) {
            service.runLowStockPatrol();
        }
    }

    @Scheduled(cron = "${app.agent.patrol.order-overdue-cron:0 0 */4 * * ?}")
    public void orderOverduePatrol() {
        if (enabled) {
            service.runOrderOverduePatrol(orderOverdueHours);
        }
    }

    @Scheduled(cron = "${app.agent.patrol.ticket-overdue-cron:0 0 * * * ?}")
    public void ticketOverduePatrol() {
        if (enabled) {
            service.runTicketOverduePatrol(ticketOverdueHours);
        }
    }
}
