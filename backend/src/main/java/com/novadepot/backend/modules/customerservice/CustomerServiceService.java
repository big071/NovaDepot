package com.novadepot.backend.modules.customerservice;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomerServiceService {
    private final CustomerConversationService conversationService;
    private final CustomerTicketService ticketService;
    private final CustomerAiSuggestionService aiSuggestionService;
    private final CustomerFaqService faqService;

    public CustomerServiceService(CustomerConversationService conversationService,
                                  CustomerTicketService ticketService,
                                  CustomerAiSuggestionService aiSuggestionService,
                                  CustomerFaqService faqService) {
        this.conversationService = conversationService;
        this.ticketService = ticketService;
        this.aiSuggestionService = aiSuggestionService;
        this.faqService = faqService;
    }

    public List<Map<String, Object>> sessions() {
        return conversationService.sessions();
    }

    public List<Map<String, Object>> messages(Long sessionId) {
        return conversationService.messages(sessionId);
    }

    public Map<String, Object> aiSuggestions(Long sessionId) {
        return aiSuggestionService.aiSuggestions(sessionId);
    }

    public Map<String, Object> sendMessage(Long sessionId,
                                           String content,
                                           String msgType,
                                           Boolean sendByAi,
                                           String senderType,
                                           Boolean autoReply) {
        return conversationService.sendMessage(sessionId, content, msgType, sendByAi, senderType, autoReply);
    }

    public Map<String, Object> transferHuman(Long sessionId, Long targetUserId) {
        return conversationService.transferHuman(sessionId, targetUserId);
    }

    public Map<String, Object> createTicket(Long sessionId, String priority, String content) {
        return ticketService.createTicket(sessionId, priority, content);
    }

    public Map<String, Object> tickets(Long sessionId, Integer pageNo, Integer pageSize) {
        return ticketService.tickets(sessionId, pageNo, pageSize);
    }

    public Map<String, Object> updateTicketStatus(Long ticketId, String status, String note) {
        return ticketService.updateTicketStatus(ticketId, status, note);
    }

    public Map<String, Object> updateTicketOwner(Long ticketId, Long assigneeUserId) {
        return ticketService.updateTicketOwner(ticketId, assigneeUserId);
    }

    public Map<String, Object> updateTicketRemark(Long ticketId, String remark) {
        return ticketService.updateTicketRemark(ticketId, remark);
    }

    public Map<String, Object> ticketTimeline(Long ticketId) {
        return ticketService.ticketTimeline(ticketId);
    }

    public List<Map<String, Object>> faq(String keyword, String scene) {
        return faqService.faq(keyword, scene);
    }

    public Map<String, Object> updateFaq(Long id, String question, String answer, String scene) {
        return faqService.updateFaq(id, question, answer, scene);
    }
}
