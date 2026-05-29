package com.novadepot.backend.modules.knowledge;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KnowledgeMatchService {
    private final KnowledgeCore core;

    public KnowledgeMatchService(KnowledgeCore core) {
        this.core = core;
    }

    public List<Map<String, Object>> matchKnowledge(String text, String scene) {
        return core.matchKnowledge(text, scene);
    }
}
