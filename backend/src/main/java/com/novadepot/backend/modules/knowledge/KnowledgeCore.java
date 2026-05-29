package com.novadepot.backend.modules.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.CustomerServiceTicketEntity;
import com.novadepot.backend.model.entity.FAQKnowledgeEntity;
import com.novadepot.backend.model.entity.RuleConfigEntity;
import com.novadepot.backend.model.entity.SopKnowledgeEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.CustomerServiceTicketMapper;
import com.novadepot.backend.repository.FAQKnowledgeMapper;
import com.novadepot.backend.repository.RuleConfigMapper;
import com.novadepot.backend.repository.SopKnowledgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeCore {
    private static final String MODULE = "KNOWLEDGE";
    private static final String APPROVED = "APPROVED";
    private static final String DRAFT = "DRAFT";

    private final FAQKnowledgeMapper faqMapper;
    private final SopKnowledgeMapper sopMapper;
    private final RuleConfigMapper ruleConfigMapper;
    private final CustomerServiceTicketMapper ticketMapper;
    private final AuthQueryMapper authQueryMapper;
    private final AuditLogRecordService auditLogRecordService;

    public KnowledgeCore(FAQKnowledgeMapper faqMapper,
                         SopKnowledgeMapper sopMapper,
                         RuleConfigMapper ruleConfigMapper,
                         CustomerServiceTicketMapper ticketMapper,
                         AuthQueryMapper authQueryMapper,
                         AuditLogRecordService auditLogRecordService) {
        this.faqMapper = faqMapper;
        this.sopMapper = sopMapper;
        this.ruleConfigMapper = ruleConfigMapper;
        this.ticketMapper = ticketMapper;
        this.authQueryMapper = authQueryMapper;
        this.auditLogRecordService = auditLogRecordService;
    }

    public List<Map<String, Object>> listFaqs(String keyword, String scene, String status) {
        LambdaQueryWrapper<FAQKnowledgeEntity> qw = new LambdaQueryWrapper<FAQKnowledgeEntity>()
                .eq(FAQKnowledgeEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(FAQKnowledgeEntity::getPriority)
                .orderByDesc(FAQKnowledgeEntity::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(FAQKnowledgeEntity::getQuestion, keyword.trim())
                    .or().like(FAQKnowledgeEntity::getAnswer, keyword.trim())
                    .or().like(FAQKnowledgeEntity::getTags, keyword.trim()));
        }
        if (StringUtils.hasText(scene)) {
            qw.eq(FAQKnowledgeEntity::getScene, scene.trim());
        }
        if (StringUtils.hasText(status)) {
            qw.eq(FAQKnowledgeEntity::getReviewStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        return faqMapper.selectList(qw).stream().map(this::faqMap).toList();
    }

    public List<Map<String, Object>> listSops(String keyword, String scene, String status) {
        LambdaQueryWrapper<SopKnowledgeEntity> qw = new LambdaQueryWrapper<SopKnowledgeEntity>()
                .eq(SopKnowledgeEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(SopKnowledgeEntity::getPriority)
                .orderByDesc(SopKnowledgeEntity::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SopKnowledgeEntity::getTitle, keyword.trim())
                    .or().like(SopKnowledgeEntity::getSteps, keyword.trim())
                    .or().like(SopKnowledgeEntity::getTags, keyword.trim()));
        }
        if (StringUtils.hasText(scene)) {
            qw.eq(SopKnowledgeEntity::getScene, scene.trim());
        }
        if (StringUtils.hasText(status)) {
            qw.eq(SopKnowledgeEntity::getReviewStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        return sopMapper.selectList(qw).stream().map(this::sopMap).toList();
    }

    public List<Map<String, Object>> listRules(String scene) {
        LambdaQueryWrapper<RuleConfigEntity> qw = new LambdaQueryWrapper<RuleConfigEntity>()
                .eq(RuleConfigEntity::getTenantId, RequestContext.tenantId())
                .orderByAsc(RuleConfigEntity::getConfigKey);
        if (StringUtils.hasText(scene)) {
            qw.eq(RuleConfigEntity::getScene, scene.trim());
        }
        return ruleConfigMapper.selectList(qw).stream().map(this::ruleMap).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createFaq(Map<String, Object> body) {
        boolean admin = isAdmin();
        FAQKnowledgeEntity row = new FAQKnowledgeEntity();
        row.setTenantId(RequestContext.tenantId());
        row.setFaqCode(NoGenerator.next("FAQ"));
        row.setQuestion(text(body.get("question"), "待完善问题", 500));
        row.setAnswer(text(body.get("answer"), "待补充答案", 3000));
        row.setScene(text(body.get("scene"), "customer-service", 64));
        row.setTags(text(body.get("tags"), "", 255));
        row.setPriority(intValue(body.get("priority"), 0));
        row.setReviewStatus(admin && "APPROVED".equalsIgnoreCase(String.valueOf(body.get("reviewStatus"))) ? APPROVED : DRAFT);
        row.setEnabled(admin && APPROVED.equals(row.getReviewStatus()) ? intValue(body.get("enabled"), 1) : 0);
        row.setVersionNo(1);
        row.setSourceType(text(body.get("sourceType"), "MANUAL", 32));
        row.setSourceRefId(text(body.get("sourceRefId"), "", 64));
        faqMapper.insert(row);
        record("FAQ_CREATE", "FAQ_KNOWLEDGE", row.getId(), row.getFaqCode(), null, faqMap(row));
        return faqMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateFaq(Long id, Map<String, Object> body) {
        FAQKnowledgeEntity row = mustFaq(id);
        if (!isAdmin() && !DRAFT.equalsIgnoreCase(nullTo(row.getReviewStatus(), APPROVED))) {
            throw new BizException(ErrorCode.FORBIDDEN.code(), "客服运营只能维护草稿，已启用知识请由管理员调整");
        }
        Map<String, Object> before = faqMap(row);
        row.setQuestion(text(body.get("question"), row.getQuestion(), 500));
        row.setAnswer(text(body.get("answer"), row.getAnswer(), 3000));
        row.setScene(text(body.get("scene"), row.getScene(), 64));
        row.setTags(text(body.get("tags"), row.getTags(), 255));
        row.setPriority(intValue(body.get("priority"), row.getPriority() == null ? 0 : row.getPriority()));
        row.setVersionNo((row.getVersionNo() == null ? 1 : row.getVersionNo()) + 1);
        faqMapper.updateById(row);
        record("FAQ_UPDATE", "FAQ_KNOWLEDGE", row.getId(), row.getFaqCode(), before, faqMap(row));
        return faqMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSop(Map<String, Object> body) {
        boolean admin = isAdmin();
        SopKnowledgeEntity row = new SopKnowledgeEntity();
        row.setTenantId(RequestContext.tenantId());
        row.setSopCode(NoGenerator.next("SOP"));
        row.setTitle(text(body.get("title"), "待完善 SOP", 255));
        row.setScene(text(body.get("scene"), "customer-service", 64));
        row.setSteps(text(body.get("steps"), "1. 确认问题；2. 分配责任人；3. 记录处理结果；4. 回访确认。", 3000));
        row.setRisks(text(body.get("risks"), "", 1000));
        row.setReviewChecks(text(body.get("reviewChecks"), "", 1000));
        row.setTags(text(body.get("tags"), "", 255));
        row.setPriority(intValue(body.get("priority"), 0));
        row.setReviewStatus(admin && "APPROVED".equalsIgnoreCase(String.valueOf(body.get("reviewStatus"))) ? APPROVED : DRAFT);
        row.setEnabled(admin && APPROVED.equals(row.getReviewStatus()) ? intValue(body.get("enabled"), 1) : 0);
        row.setSourceType(text(body.get("sourceType"), "MANUAL", 32));
        row.setSourceRefId(text(body.get("sourceRefId"), "", 64));
        sopMapper.insert(row);
        record("SOP_CREATE", "SOP_KNOWLEDGE", row.getId(), row.getSopCode(), null, sopMap(row));
        return sopMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSop(Long id, Map<String, Object> body) {
        SopKnowledgeEntity row = mustSop(id);
        if (!isAdmin() && !DRAFT.equalsIgnoreCase(nullTo(row.getReviewStatus(), APPROVED))) {
            throw new BizException(ErrorCode.FORBIDDEN.code(), "客服运营只能维护草稿，已启用 SOP 请由管理员调整");
        }
        Map<String, Object> before = sopMap(row);
        row.setTitle(text(body.get("title"), row.getTitle(), 255));
        row.setScene(text(body.get("scene"), row.getScene(), 64));
        row.setSteps(text(body.get("steps"), row.getSteps(), 3000));
        row.setRisks(text(body.get("risks"), row.getRisks(), 1000));
        row.setReviewChecks(text(body.get("reviewChecks"), row.getReviewChecks(), 1000));
        row.setTags(text(body.get("tags"), row.getTags(), 255));
        row.setPriority(intValue(body.get("priority"), row.getPriority() == null ? 0 : row.getPriority()));
        sopMapper.updateById(row);
        record("SOP_UPDATE", "SOP_KNOWLEDGE", row.getId(), row.getSopCode(), before, sopMap(row));
        return sopMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmFaq(Long id) {
        requireAdmin("只有管理员可以确认 FAQ 草稿并启用");
        FAQKnowledgeEntity row = mustFaq(id);
        Map<String, Object> before = faqMap(row);
        row.setReviewStatus(APPROVED);
        row.setEnabled(1);
        faqMapper.updateById(row);
        record("FAQ_CONFIRM", "FAQ_KNOWLEDGE", row.getId(), row.getFaqCode(), before, faqMap(row));
        return faqMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmSop(Long id) {
        requireAdmin("只有管理员可以确认 SOP 草稿并启用");
        SopKnowledgeEntity row = mustSop(id);
        Map<String, Object> before = sopMap(row);
        row.setReviewStatus(APPROVED);
        row.setEnabled(1);
        sopMapper.updateById(row);
        record("SOP_CONFIRM", "SOP_KNOWLEDGE", row.getId(), row.getSopCode(), before, sopMap(row));
        return sopMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> switchFaq(Long id, boolean enabled) {
        requireAdmin("只有管理员可以启用或停用 FAQ");
        FAQKnowledgeEntity row = mustFaq(id);
        Map<String, Object> before = faqMap(row);
        row.setEnabled(enabled ? 1 : 0);
        faqMapper.updateById(row);
        record(enabled ? "FAQ_ENABLE" : "FAQ_DISABLE", "FAQ_KNOWLEDGE", row.getId(), row.getFaqCode(), before, faqMap(row));
        return faqMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> switchSop(Long id, boolean enabled) {
        requireAdmin("只有管理员可以启用或停用 SOP");
        SopKnowledgeEntity row = mustSop(id);
        Map<String, Object> before = sopMap(row);
        row.setEnabled(enabled ? 1 : 0);
        sopMapper.updateById(row);
        record(enabled ? "SOP_ENABLE" : "SOP_DISABLE", "SOP_KNOWLEDGE", row.getId(), row.getSopCode(), before, sopMap(row));
        return sopMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateRule(String configKey, Map<String, Object> body) {
        requireAdmin("只有管理员可以维护规则配置");
        RuleConfigEntity row = ruleConfigMapper.selectByConfigKey(RequestContext.tenantId(), configKey);
        if (row == null) {
            row = new RuleConfigEntity();
            row.setTenantId(RequestContext.tenantId());
            row.setConfigKey(configKey);
            row.setConfigName(text(body.get("configName"), configKey, 128));
            row.setCreatedBy(RequestContext.userId());
        }
        Map<String, Object> before = row.getId() == null ? null : ruleMap(row);
        row.setConfigName(text(body.get("configName"), row.getConfigName(), 128));
        row.setConfigValue(text(body.get("configValue"), row.getConfigValue(), 3000));
        row.setValueType(text(body.get("valueType"), row.getValueType() == null ? "TEXT" : row.getValueType(), 16));
        row.setScene(text(body.get("scene"), row.getScene(), 64));
        row.setRemark(text(body.get("remark"), row.getRemark(), 500));
        row.setEnabled(intValue(body.get("enabled"), row.getEnabled() == null ? 1 : row.getEnabled()));
        if (row.getId() == null) {
            ruleConfigMapper.insert(row);
        } else {
            ruleConfigMapper.updateById(row);
        }
        record("RULE_CONFIG_UPDATE", "RULE_CONFIG", row.getId(), row.getConfigKey(), before, ruleMap(row));
        return ruleMap(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> draftFaqFromTicket(Long ticketId) {
        CustomerServiceTicketEntity ticket = mustTicket(ticketId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", "如何处理：" + text(ticket.getContent(), "客户问题", 180));
        body.put("answer", "请先确认客户诉求，补充处理备注，必要时人工接管并同步下一步时间。");
        body.put("scene", "customer-service");
        body.put("tags", "工单沉淀,客服");
        body.put("priority", 10);
        body.put("sourceType", "CS_TICKET");
        body.put("sourceRefId", ticket.getTicketNo());
        Map<String, Object> draft = createFaq(body);
        record("FAQ_DRAFT_FROM_TICKET", "CS_TICKET", ticket.getId(), ticket.getTicketNo(), null, draft);
        return draft;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> draftSopFromTicket(Long ticketId) {
        CustomerServiceTicketEntity ticket = mustTicket(ticketId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "工单处理 SOP：" + text(ticket.getContent(), "客户问题", 120));
        body.put("scene", "customer-service");
        body.put("steps", "1. 确认客户问题；2. 明确责任人；3. 记录处理备注；4. 同步下一步时间；5. 关闭前填写原因并回访。");
        body.put("risks", "问题分类不清、责任人缺失、关闭原因不完整。");
        body.put("reviewChecks", "是否有责任人；是否有处理备注；是否有关闭原因。");
        body.put("tags", "工单沉淀,客服,SOP");
        body.put("priority", 10);
        body.put("sourceType", "CS_TICKET");
        body.put("sourceRefId", ticket.getTicketNo());
        Map<String, Object> draft = createSop(body);
        record("SOP_DRAFT_FROM_TICKET", "CS_TICKET", ticket.getId(), ticket.getTicketNo(), null, draft);
        return draft;
    }

    public List<Map<String, Object>> matchKnowledge(String text, String scene) {
        String source = text(text, "", 1000).toLowerCase(Locale.ROOT);
        List<Map<String, Object>> refs = new ArrayList<>();
        for (FAQKnowledgeEntity faq : activeFaqs(scene)) {
            List<String> tags = splitTags(faq.getTags());
            boolean matched = containsAny(source, tags) || source.contains(nullTo(faq.getQuestion(), "").toLowerCase(Locale.ROOT));
            if (matched || refs.isEmpty()) {
                Map<String, Object> ref = baseRef("FAQ", faq.getFaqCode(), faq.getQuestion(), faq.getScene(), tags);
                ref.put("answer", faq.getAnswer());
                ref.put("reason", matched ? "客户问题命中 FAQ 标签或问题描述" : "未精确命中，按优先级推荐 FAQ");
                ref.put("nextAction", "优先参考 FAQ 答案生成回复");
                refs.add(ref);
            }
            if (refs.size() >= 2) break;
        }
        for (SopKnowledgeEntity sop : activeSops(scene)) {
            List<String> tags = splitTags(sop.getTags());
            boolean matched = containsAny(source, tags) || source.contains(nullTo(sop.getTitle(), "").toLowerCase(Locale.ROOT));
            if (matched || refs.stream().noneMatch(r -> "SOP".equals(r.get("type")))) {
                Map<String, Object> ref = baseRef("SOP", sop.getSopCode(), sop.getTitle(), sop.getScene(), tags);
                ref.put("steps", sop.getSteps());
                ref.put("risks", nullTo(sop.getRisks(), ""));
                ref.put("reviewChecks", nullTo(sop.getReviewChecks(), ""));
                ref.put("reason", matched ? "业务内容命中 SOP 标签或标题" : "未精确命中，按优先级推荐 SOP");
                ref.put("nextAction", "按 SOP 步骤处理并补充备注");
                refs.add(ref);
            }
            if (refs.size() >= 4) break;
        }
        return refs;
    }

    public BigDecimal decimalRule(String key, BigDecimal fallback) {
        RuleConfigEntity row = activeRule(key);
        if (row == null) return fallback;
        try {
            return new BigDecimal(row.getConfigValue());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public int intRule(String key, int fallback) {
        RuleConfigEntity row = activeRule(key);
        if (row == null) return fallback;
        try {
            return Integer.parseInt(row.getConfigValue());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public String textRule(String key, String fallback) {
        RuleConfigEntity row = activeRule(key);
        return row == null || !StringUtils.hasText(row.getConfigValue()) ? fallback : row.getConfigValue();
    }

    private List<FAQKnowledgeEntity> activeFaqs(String scene) {
        return faqMapper.selectActiveForMatch(RequestContext.tenantId(), StringUtils.hasText(scene) ? scene : null, 20);
    }

    private List<SopKnowledgeEntity> activeSops(String scene) {
        return sopMapper.selectActiveForMatch(RequestContext.tenantId(), StringUtils.hasText(scene) ? scene : null, 20);
    }

    private RuleConfigEntity activeRule(String key) {
        return ruleConfigMapper.selectActiveByConfigKey(RequestContext.tenantId(), key);
    }

    private FAQKnowledgeEntity mustFaq(Long id) {
        FAQKnowledgeEntity row = faqMapper.selectOne(new LambdaQueryWrapper<FAQKnowledgeEntity>()
                .eq(FAQKnowledgeEntity::getTenantId, RequestContext.tenantId())
                .eq(FAQKnowledgeEntity::getId, id));
        if (row == null) throw new BizException(ErrorCode.BIZ_ERROR.code(), "FAQ 不存在");
        return row;
    }

    private SopKnowledgeEntity mustSop(Long id) {
        SopKnowledgeEntity row = sopMapper.selectOne(new LambdaQueryWrapper<SopKnowledgeEntity>()
                .eq(SopKnowledgeEntity::getTenantId, RequestContext.tenantId())
                .eq(SopKnowledgeEntity::getId, id));
        if (row == null) throw new BizException(ErrorCode.BIZ_ERROR.code(), "SOP 不存在");
        return row;
    }

    private CustomerServiceTicketEntity mustTicket(Long id) {
        CustomerServiceTicketEntity row = ticketMapper.selectOne(new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId())
                .eq(CustomerServiceTicketEntity::getId, id));
        if (row == null) throw new BizException(ErrorCode.BIZ_ERROR.code(), "工单不存在");
        return row;
    }

    private Map<String, Object> faqMap(FAQKnowledgeEntity row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row.getId());
        map.put("faqCode", row.getFaqCode());
        map.put("question", row.getQuestion());
        map.put("answer", row.getAnswer());
        map.put("tags", nullTo(row.getTags(), ""));
        map.put("scene", nullTo(row.getScene(), ""));
        map.put("priority", row.getPriority() == null ? 0 : row.getPriority());
        map.put("enabled", row.getEnabled() == null ? 0 : row.getEnabled());
        map.put("reviewStatus", nullTo(row.getReviewStatus(), APPROVED));
        map.put("sourceType", nullTo(row.getSourceType(), ""));
        map.put("sourceRefId", nullTo(row.getSourceRefId(), ""));
        return map;
    }

    private Map<String, Object> sopMap(SopKnowledgeEntity row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row.getId());
        map.put("sopCode", row.getSopCode());
        map.put("title", row.getTitle());
        map.put("scene", nullTo(row.getScene(), ""));
        map.put("steps", row.getSteps());
        map.put("risks", nullTo(row.getRisks(), ""));
        map.put("reviewChecks", nullTo(row.getReviewChecks(), ""));
        map.put("tags", nullTo(row.getTags(), ""));
        map.put("priority", row.getPriority() == null ? 0 : row.getPriority());
        map.put("enabled", row.getEnabled() == null ? 0 : row.getEnabled());
        map.put("reviewStatus", nullTo(row.getReviewStatus(), APPROVED));
        map.put("sourceType", nullTo(row.getSourceType(), ""));
        map.put("sourceRefId", nullTo(row.getSourceRefId(), ""));
        return map;
    }

    private Map<String, Object> ruleMap(RuleConfigEntity row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", row.getId());
        map.put("configKey", row.getConfigKey());
        map.put("configName", row.getConfigName());
        map.put("configValue", row.getConfigValue());
        map.put("valueType", row.getValueType());
        map.put("scene", nullTo(row.getScene(), ""));
        map.put("remark", nullTo(row.getRemark(), ""));
        map.put("enabled", row.getEnabled() == null ? 0 : row.getEnabled());
        return map;
    }

    private Map<String, Object> baseRef(String type, String code, String title, String scene, List<String> tags) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("type", type);
        ref.put("code", code);
        ref.put("title", title);
        ref.put("scene", nullTo(scene, ""));
        ref.put("matchedTags", tags);
        return ref;
    }

    private void record(String action, String resourceType, Long id, String bizNo, Object before, Object after) {
        auditLogRecordService.record(MODULE, action, resourceType, String.valueOf(id), bizNo, toJson(before), toJson(after));
    }

    private String toJson(Object data) {
        if (data == null) return null;
        String raw = String.valueOf(data);
        return "{\"summary\":\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private void requireAdmin(String message) {
        if (!isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN.code(), message);
        }
    }

    private boolean isAdmin() {
        return roles().contains("TENANT_ADMIN") || roles().contains("admin");
    }

    private Set<String> roles() {
        Long userId = RequestContext.userId();
        if (userId == null) return Set.of();
        return Set.copyOf(authQueryMapper.findRoleCodes(RequestContext.tenantId(), userId));
    }

    private boolean containsAny(String source, List<String> words) {
        if (!StringUtils.hasText(source)) return false;
        for (String word : words) {
            if (StringUtils.hasText(word) && source.contains(word.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) return List.of();
        return List.of(tags.split("[,，;；]")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String text(Object value, String fallback, int maxLen) {
        String raw = value == null ? "" : String.valueOf(value).trim();
        if (!StringUtils.hasText(raw)) raw = fallback == null ? "" : fallback;
        return raw.length() <= maxLen ? raw : raw.substring(0, maxLen);
    }

    private int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String nullTo(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
