package com.novadepot.backend.modules.customerservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CustomerConversationService {
    private final CustomerServiceCore core;

    public CustomerConversationService(CustomerServiceCore core) {
        this.core = core;
    }

    public List<Map<String, Object>> sessions() {
        return core.sessions();
    }

    public List<Map<String, Object>> messages(Long sessionId) {
        return core.messages(sessionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendMessage(Long sessionId,
                                           String content,
                                           String msgType,
                                           Boolean sendByAi,
                                           String senderType,
                                           Boolean autoReply) {
        return core.sendMessage(sessionId, content, msgType, sendByAi, senderType, autoReply);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> transferHuman(Long sessionId, Long targetUserId) {
        return core.transferHuman(sessionId, targetUserId);
    }
}
