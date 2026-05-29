package com.novadepot.backend.modules.customerservice;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomerAiSuggestionService {
    private final CustomerServiceCore core;

    public CustomerAiSuggestionService(CustomerServiceCore core) {
        this.core = core;
    }

    public Map<String, Object> aiSuggestions(Long sessionId) {
        return core.aiSuggestions(sessionId);
    }
}
