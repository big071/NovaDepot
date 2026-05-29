package com.novadepot.backend.modules.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class KnowledgeSopService {
    private final KnowledgeCore core;

    public KnowledgeSopService(KnowledgeCore core) {
        this.core = core;
    }

    public List<Map<String, Object>> listSops(String keyword, String scene, String status) {
        return core.listSops(keyword, scene, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSop(Map<String, Object> body) {
        return core.createSop(body);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSop(Long id, Map<String, Object> body) {
        return core.updateSop(id, body);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmSop(Long id) {
        return core.confirmSop(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> switchSop(Long id, boolean enabled) {
        return core.switchSop(id, enabled);
    }
}
