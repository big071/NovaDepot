package com.novadepot.backend.modules.customerservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CustomerFaqService {
    private final CustomerServiceCore core;

    public CustomerFaqService(CustomerServiceCore core) {
        this.core = core;
    }

    public List<Map<String, Object>> faq(String keyword, String scene) {
        return core.faq(keyword, scene);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateFaq(Long id, String question, String answer, String scene) {
        return core.updateFaq(id, question, answer, scene);
    }
}
