package com.novadepot.backend.modules.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeRuleService {
    private final KnowledgeCore core;

    public KnowledgeRuleService(KnowledgeCore core) {
        this.core = core;
    }

    public List<Map<String, Object>> listRules(String scene) {
        return core.listRules(scene);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateRule(String configKey, Map<String, Object> body) {
        return core.updateRule(configKey, body);
    }

    public BigDecimal decimalRule(String key, BigDecimal fallback) {
        return core.decimalRule(key, fallback);
    }

    public int intRule(String key, int fallback) {
        return core.intRule(key, fallback);
    }

    public String textRule(String key, String fallback) {
        return core.textRule(key, fallback);
    }
}
