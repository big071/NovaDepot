package com.novadepot.backend.modules.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CustomerKnowledgeDraftService {
    private final KnowledgeCore core;

    public CustomerKnowledgeDraftService(KnowledgeCore core) {
        this.core = core;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> draftFaqFromTicket(Long ticketId) {
        return core.draftFaqFromTicket(ticketId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> draftSopFromTicket(Long ticketId) {
        return core.draftSopFromTicket(ticketId);
    }
}
