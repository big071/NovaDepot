package com.novadepot.backend.modules.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class KnowledgeFaqService {
    private final KnowledgeCore core;

    public KnowledgeFaqService(KnowledgeCore core) {
        this.core = core;
    }

    public List<Map<String, Object>> listFaqs(String keyword, String scene, String status) {
        return core.listFaqs(keyword, scene, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createFaq(Map<String, Object> body) {
        return core.createFaq(body);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateFaq(Long id, Map<String, Object> body) {
        return core.updateFaq(id, body);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmFaq(Long id) {
        return core.confirmFaq(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> switchFaq(Long id, boolean enabled) {
        return core.switchFaq(id, enabled);
    }
}
