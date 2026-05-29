package com.novadepot.backend.modules.customerservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CustomerTicketService {
    private final CustomerServiceCore core;

    public CustomerTicketService(CustomerServiceCore core) {
        this.core = core;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createTicket(Long sessionId, String priority, String content) {
        return core.createTicket(sessionId, priority, content);
    }

    public Map<String, Object> tickets(Long sessionId, Integer pageNo, Integer pageSize) {
        return core.tickets(sessionId, pageNo, pageSize);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTicketStatus(Long ticketId, String status, String note) {
        return core.updateTicketStatus(ticketId, status, note);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTicketOwner(Long ticketId, Long assigneeUserId) {
        return core.updateTicketOwner(ticketId, assigneeUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTicketRemark(Long ticketId, String remark) {
        return core.updateTicketRemark(ticketId, remark);
    }

    public Map<String, Object> ticketTimeline(Long ticketId) {
        return core.ticketTimeline(ticketId);
    }
}
