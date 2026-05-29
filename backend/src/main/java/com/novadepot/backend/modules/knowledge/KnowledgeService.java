package com.novadepot.backend.modules.knowledge;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeService {
    private final KnowledgeFaqService faqService;
    private final KnowledgeSopService sopService;
    private final KnowledgeRuleService ruleService;
    private final CustomerKnowledgeDraftService draftService;
    private final KnowledgeMatchService matchService;

    public KnowledgeService(KnowledgeFaqService faqService,
                            KnowledgeSopService sopService,
                            KnowledgeRuleService ruleService,
                            CustomerKnowledgeDraftService draftService,
                            KnowledgeMatchService matchService) {
        this.faqService = faqService;
        this.sopService = sopService;
        this.ruleService = ruleService;
        this.draftService = draftService;
        this.matchService = matchService;
    }

    public List<Map<String, Object>> listFaqs(String keyword, String scene, String status) {
        return faqService.listFaqs(keyword, scene, status);
    }

    public Map<String, Object> createFaq(Map<String, Object> body) {
        return faqService.createFaq(body);
    }

    public Map<String, Object> updateFaq(Long id, Map<String, Object> body) {
        return faqService.updateFaq(id, body);
    }

    public Map<String, Object> confirmFaq(Long id) {
        return faqService.confirmFaq(id);
    }

    public Map<String, Object> switchFaq(Long id, boolean enabled) {
        return faqService.switchFaq(id, enabled);
    }

    public List<Map<String, Object>> listSops(String keyword, String scene, String status) {
        return sopService.listSops(keyword, scene, status);
    }

    public Map<String, Object> createSop(Map<String, Object> body) {
        return sopService.createSop(body);
    }

    public Map<String, Object> updateSop(Long id, Map<String, Object> body) {
        return sopService.updateSop(id, body);
    }

    public Map<String, Object> confirmSop(Long id) {
        return sopService.confirmSop(id);
    }

    public Map<String, Object> switchSop(Long id, boolean enabled) {
        return sopService.switchSop(id, enabled);
    }

    public List<Map<String, Object>> listRules(String scene) {
        return ruleService.listRules(scene);
    }

    public Map<String, Object> updateRule(String configKey, Map<String, Object> body) {
        return ruleService.updateRule(configKey, body);
    }

    public Map<String, Object> draftFaqFromTicket(Long ticketId) {
        return draftService.draftFaqFromTicket(ticketId);
    }

    public Map<String, Object> draftSopFromTicket(Long ticketId) {
        return draftService.draftSopFromTicket(ticketId);
    }

    public List<Map<String, Object>> matchKnowledge(String text, String scene) {
        return matchService.matchKnowledge(text, scene);
    }

    public BigDecimal decimalRule(String key, BigDecimal fallback) {
        return ruleService.decimalRule(key, fallback);
    }

    public int intRule(String key, int fallback) {
        return ruleService.intRule(key, fallback);
    }

    public String textRule(String key, String fallback) {
        return ruleService.textRule(key, fallback);
    }
}
