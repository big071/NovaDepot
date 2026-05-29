package com.novadepot.backend.modules.customerservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.utils.NoGenerator;
import com.novadepot.backend.model.entity.BusinessHistoryEventEntity;
import com.novadepot.backend.model.entity.CustomerServiceMessageEntity;
import com.novadepot.backend.model.entity.CustomerServiceSessionEntity;
import com.novadepot.backend.model.entity.CustomerServiceTicketEntity;
import com.novadepot.backend.model.entity.FAQKnowledgeEntity;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.auth.AuthUserRow;
import com.novadepot.backend.modules.knowledge.KnowledgeService;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.repository.BusinessHistoryEventMapper;
import com.novadepot.backend.repository.CustomerServiceMessageMapper;
import com.novadepot.backend.repository.CustomerServiceSessionMapper;
import com.novadepot.backend.repository.CustomerServiceTicketMapper;
import com.novadepot.backend.repository.FAQKnowledgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CustomerServiceCore {
    private static final String MODULE = "CUSTOMER_SERVICE";
    private static final String RESOURCE_TICKET = "CS_TICKET";
    private static final String RESOURCE_SESSION = "CS_SESSION";
    private static final Set<String> TICKET_STATUS = Set.of("OPEN", "PROCESSING", "RESOLVED", "CLOSED");

    private final CustomerServiceSessionMapper sessionMapper;
    private final CustomerServiceMessageMapper messageMapper;
    private final CustomerServiceTicketMapper ticketMapper;
    private final FAQKnowledgeMapper faqMapper;
    private final BusinessHistoryEventMapper historyMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final AuthQueryMapper authQueryMapper;
    private final KnowledgeService knowledgeService;

    public CustomerServiceCore(CustomerServiceSessionMapper sessionMapper,
                               CustomerServiceMessageMapper messageMapper,
                               CustomerServiceTicketMapper ticketMapper,
                               FAQKnowledgeMapper faqMapper,
                               BusinessHistoryEventMapper historyMapper,
                               AuditLogRecordService auditLogRecordService,
                               AuthQueryMapper authQueryMapper,
                               KnowledgeService knowledgeService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
        this.faqMapper = faqMapper;
        this.historyMapper = historyMapper;
        this.auditLogRecordService = auditLogRecordService;
        this.authQueryMapper = authQueryMapper;
        this.knowledgeService = knowledgeService;
    }

    public List<Map<String, Object>> sessions() {
        List<CustomerServiceSessionEntity> rows = sessionMapper.selectList(new LambdaQueryWrapper<CustomerServiceSessionEntity>()
                .eq(CustomerServiceSessionEntity::getTenantId, RequestContext.tenantId())
                .orderByDesc(CustomerServiceSessionEntity::getCreatedAt)
                .orderByDesc(CustomerServiceSessionEntity::getId));
        return rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("sessionNo", row.getSessionNo());
            item.put("status", normalizeSessionStatus(row.getStatus()));
            item.put("priority", normalizePriority(row.getPriority()));
            item.put("assignedUserId", row.getAssignedUserId());
            item.put("handoffStatus", hasHistory(RESOURCE_SESSION, String.valueOf(row.getId()), "HUMAN_TAKEOVER") ? "HUMAN_ASSIGNED" : "AI_FIRST");
            return item;
        }).toList();
    }

    public List<Map<String, Object>> messages(Long sessionId) {
        CustomerServiceSessionEntity session = mustGetSession(sessionId);
        List<CustomerServiceMessageEntity> rows = messageMapper.selectList(new LambdaQueryWrapper<CustomerServiceMessageEntity>()
                .eq(CustomerServiceMessageEntity::getTenantId, RequestContext.tenantId())
                .eq(CustomerServiceMessageEntity::getSessionId, session.getId())
                .orderByAsc(CustomerServiceMessageEntity::getCreatedAt)
                .orderByAsc(CustomerServiceMessageEntity::getId));
        return rows.stream().map(this::toMessageMap).toList();
    }

    public Map<String, Object> aiSuggestions(Long sessionId) {
        return buildSuggestion(mustGetSession(sessionId), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendMessage(Long sessionId,
                                           String content,
                                           String msgType,
                                           Boolean sendByAi,
                                           String senderType,
                                           Boolean autoReply) {
        CustomerServiceSessionEntity session = mustGetSession(sessionId);
        String sender = normalizeSenderInput(senderType, sendByAi);
        CustomerServiceMessageEntity saved = insertMessage(session, sender, normalizeText(content, "", 2000), msgType);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", toMessageMap(saved));
        result.put("autoReplyEnabled", false);

        if ("CUSTOMER".equals(sender) && Boolean.TRUE.equals(autoReply)) {
            Map<String, Object> suggestion = buildSuggestion(session, false);
            List<?> candidates = (List<?>) suggestion.get("replyCandidates");
            String autoText = candidates == null || candidates.isEmpty() ? "我们已收到您的问题，正在处理中。" : String.valueOf(candidates.get(0));
            CustomerServiceMessageEntity auto = insertMessage(session, "AI", autoText, "AI_AUTO_REPLY");
            result.put("autoReply", toMessageMap(auto));
            result.put("autoReplyEnabled", true);
            result.put("suggestion", suggestion);
            recordHistory(RESOURCE_SESSION, String.valueOf(session.getId()), session.getSessionNo(),
                    "AI_AUTO_REPLY", "AI 自动回复", session.getStatus(), session.getStatus(), "系统自动生成回复", null, "AI");
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> transferHuman(Long sessionId, Long targetUserId) {
        CustomerServiceSessionEntity session = mustGetSession(sessionId);
        String beforeStatus = session.getStatus();
        Long beforeOwner = session.getAssignedUserId();
        Long nextOwner = targetUserId == null ? safeOperatorId() : targetUserId;
        session.setAssignedUserId(nextOwner);
        session.setStatus("PROCESSING");
        sessionMapper.updateById(session);

        String note = "责任人从 " + displayUser(beforeOwner) + " 调整为 " + displayUser(nextOwner);
        recordHistory(RESOURCE_SESSION, String.valueOf(session.getId()), session.getSessionNo(),
                "HUMAN_TAKEOVER", "人工接管", beforeStatus, session.getStatus(), note, safeOperatorId(), null);
        recordAudit("TRANSFER_HUMAN", RESOURCE_SESSION, String.valueOf(session.getId()), session.getSessionNo(),
                beforeStatus, session.getStatus(), "人工接管", note);

        return Map.of(
                "sessionId", session.getId(),
                "assignedUserId", session.getAssignedUserId(),
                "status", session.getStatus(),
                "handoffStatus", "HUMAN_ASSIGNED"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createTicket(Long sessionId, String priority, String content) {
        CustomerServiceSessionEntity session = mustGetSession(sessionId);
        CustomerServiceTicketEntity ticket = new CustomerServiceTicketEntity();
        ticket.setTenantId(RequestContext.tenantId());
        ticket.setTicketNo(NoGenerator.next("TCK"));
        ticket.setSessionId(session.getId());
        ticket.setPriority(normalizePriority(priority));
        ticket.setContent(normalizeText(content, "待客服进一步跟进", 1000));
        ticket.setStatus("OPEN");
        ticket.setAssigneeUserId(session.getAssignedUserId() == null ? safeOperatorId() : session.getAssignedUserId());
        ticket.setRemark("");
        ticketMapper.insert(ticket);

        recordHistory(RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                "CREATE", "创建工单", "-", "OPEN", ticket.getContent(), safeOperatorId(), null);
        recordAudit("TICKET_CREATE", RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                null, "OPEN", "创建工单", ticket.getContent());
        return toTicketMap(ticket, hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "AI_AUTO_REPLY"),
                hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "HUMAN_TAKEOVER"), "先补充处理备注并确认责任人");
    }
    public Map<String, Object> tickets(Long sessionId, Integer pageNo, Integer pageSize) {
        int safePageNo = Math.max(pageNo == null ? 1 : pageNo, 1);
        int safePageSize = Math.max(1, Math.min(50, pageSize == null ? 10 : pageSize));
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<CustomerServiceTicketEntity> countQw = new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId());
        if (sessionId != null) countQw.eq(CustomerServiceTicketEntity::getSessionId, sessionId);
        long total = ticketMapper.selectCount(countQw);

        List<CustomerServiceTicketEntity> rows = ticketMapper.selectTicketsPage(RequestContext.tenantId(), sessionId, offset, safePageSize);
        List<Map<String, Object>> list = new ArrayList<>();
        for (CustomerServiceTicketEntity ticket : rows) {
            CustomerServiceSessionEntity session = mustGetSession(ticket.getSessionId());
            boolean ai = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "AI_AUTO_REPLY");
            boolean human = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "HUMAN_TAKEOVER");
            String next = String.valueOf(buildSuggestion(session, false).get("recommendedAction"));
            list.add(toTicketMap(ticket, ai, human, next));
        }

        return Map.of("list", list, "total", total, "pageNo", safePageNo, "pageSize", safePageSize, "dataSource", "MYSQL");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTicketStatus(Long ticketId, String status, String note) {
        CustomerServiceTicketEntity ticket = mustGetTicket(ticketId);
        CustomerServiceSessionEntity session = mustGetSession(ticket.getSessionId());
        String nextStatus = normalizeTicketStatus(status, ticket.getStatus());
        if ("CLOSED".equals(nextStatus) && !StringUtils.hasText(note)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "关闭工单请填写关闭原因");
        }

        String beforeStatus = ticket.getStatus();
        ticket.setStatus(nextStatus);
        if (StringUtils.hasText(note)) ticket.setRemark(normalizeText(note, "", 500));
        ticketMapper.updateById(ticket);

        String label = "CLOSED".equals(nextStatus) ? "关闭工单" : "状态变更";
        String eventNote = StringUtils.hasText(note) ? normalizeText(note, "", 500) : ("状态更新为 " + nextStatus);
        recordHistory(RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                "STATUS_CHANGE", label, beforeStatus, nextStatus, eventNote, safeOperatorId(), null);
        recordAudit("TICKET_STATUS_UPDATE", RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                beforeStatus, nextStatus, label, eventNote);

        boolean ai = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "AI_AUTO_REPLY");
        boolean human = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "HUMAN_TAKEOVER");
        String next = String.valueOf(buildSuggestion(session, false).get("recommendedAction"));
        return toTicketMap(ticket, ai, human, next);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTicketOwner(Long ticketId, Long assigneeUserId) {
        if (assigneeUserId == null || assigneeUserId <= 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "责任人不能为空");
        }
        CustomerServiceTicketEntity ticket = mustGetTicket(ticketId);
        CustomerServiceSessionEntity session = mustGetSession(ticket.getSessionId());
        Long beforeOwner = ticket.getAssigneeUserId();

        ticket.setAssigneeUserId(assigneeUserId);
        ticketMapper.updateById(ticket);

        String note = "责任人从 " + displayUser(beforeOwner) + " 调整为 " + displayUser(assigneeUserId);
        recordHistory(RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                "OWNER_CHANGE", "责任人变更", ticket.getStatus(), ticket.getStatus(), note, safeOperatorId(), null);
        recordAudit("TICKET_OWNER_UPDATE", RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                ticket.getStatus(), ticket.getStatus(), "责任人变更", note);

        boolean ai = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "AI_AUTO_REPLY");
        boolean human = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "HUMAN_TAKEOVER");
        String next = String.valueOf(buildSuggestion(session, false).get("recommendedAction"));
        return toTicketMap(ticket, ai, human, next);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTicketRemark(Long ticketId, String remark) {
        CustomerServiceTicketEntity ticket = mustGetTicket(ticketId);
        CustomerServiceSessionEntity session = mustGetSession(ticket.getSessionId());
        String safeRemark = normalizeText(remark, "", 500);
        ticket.setRemark(safeRemark);
        ticketMapper.updateById(ticket);

        String note = StringUtils.hasText(safeRemark) ? safeRemark : "清空备注";
        recordHistory(RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                "REMARK", "处理备注", ticket.getStatus(), ticket.getStatus(), note, safeOperatorId(), null);
        recordAudit("TICKET_REMARK_UPDATE", RESOURCE_TICKET, String.valueOf(ticket.getId()), ticket.getTicketNo(),
                ticket.getStatus(), ticket.getStatus(), "处理备注", note);

        boolean ai = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "AI_AUTO_REPLY");
        boolean human = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "HUMAN_TAKEOVER");
        String next = String.valueOf(buildSuggestion(session, false).get("recommendedAction"));
        return toTicketMap(ticket, ai, human, next);
    }

    public Map<String, Object> ticketTimeline(Long ticketId) {
        CustomerServiceTicketEntity ticket = mustGetTicket(ticketId);
        CustomerServiceSessionEntity session = mustGetSession(ticket.getSessionId());
        List<BusinessHistoryEventEntity> ticketEvents = historyMapper.selectList(new LambdaQueryWrapper<BusinessHistoryEventEntity>()
                .eq(BusinessHistoryEventEntity::getTenantId, RequestContext.tenantId())
                .eq(BusinessHistoryEventEntity::getResourceType, RESOURCE_TICKET)
                .eq(BusinessHistoryEventEntity::getResourceId, String.valueOf(ticket.getId()))
                .orderByAsc(BusinessHistoryEventEntity::getOccurredAt)
                .orderByAsc(BusinessHistoryEventEntity::getId));
        List<BusinessHistoryEventEntity> sessionEvents = historyMapper.selectList(new LambdaQueryWrapper<BusinessHistoryEventEntity>()
                .eq(BusinessHistoryEventEntity::getTenantId, RequestContext.tenantId())
                .eq(BusinessHistoryEventEntity::getResourceType, RESOURCE_SESSION)
                .eq(BusinessHistoryEventEntity::getResourceId, String.valueOf(session.getId()))
                .in(BusinessHistoryEventEntity::getAction, List.of("AI_AUTO_REPLY", "HUMAN_TAKEOVER", "AI_SUGGESTION"))
                .orderByAsc(BusinessHistoryEventEntity::getOccurredAt)
                .orderByAsc(BusinessHistoryEventEntity::getId));

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (BusinessHistoryEventEntity row : ticketEvents) timeline.add(toTimelineMap(row));
        for (BusinessHistoryEventEntity row : sessionEvents) timeline.add(toTimelineMap(row));
        timeline.sort((a, b) -> String.valueOf(a.get("occurredAt")).compareTo(String.valueOf(b.get("occurredAt"))));

        boolean ai = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "AI_AUTO_REPLY");
        boolean human = hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "HUMAN_TAKEOVER");
        String next = String.valueOf(buildSuggestion(session, false).get("recommendedAction"));
        return Map.of(
                "ticket", toTicketMap(ticket, ai, human, next),
                "timeline", timeline,
                "auditQuery", Map.of("bizNo", ticket.getTicketNo(), "resourceType", RESOURCE_TICKET, "resourceId", String.valueOf(ticket.getId()))
        );
    }

    public List<Map<String, Object>> faq(String keyword, String scene) {
        LambdaQueryWrapper<FAQKnowledgeEntity> qw = new LambdaQueryWrapper<FAQKnowledgeEntity>()
                .eq(FAQKnowledgeEntity::getTenantId, RequestContext.tenantId())
                .eq(FAQKnowledgeEntity::getEnabled, 1)
                .eq(FAQKnowledgeEntity::getReviewStatus, "APPROVED")
                .orderByDesc(FAQKnowledgeEntity::getPriority)
                .orderByDesc(FAQKnowledgeEntity::getId);
        if (StringUtils.hasText(keyword)) qw.like(FAQKnowledgeEntity::getQuestion, keyword.trim());
        if (StringUtils.hasText(scene)) qw.eq(FAQKnowledgeEntity::getScene, scene.trim());
        return faqMapper.selectList(qw).stream().map(item -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", item.getId());
            result.put("question", item.getQuestion());
            result.put("answer", item.getAnswer());
            result.put("scene", item.getScene() == null ? "" : item.getScene());
            return result;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateFaq(Long id, String question, String answer, String scene) {
        FAQKnowledgeEntity row = faqMapper.selectOne(new LambdaQueryWrapper<FAQKnowledgeEntity>()
                .eq(FAQKnowledgeEntity::getTenantId, RequestContext.tenantId())
                .eq(FAQKnowledgeEntity::getId, id));
        if (row == null) throw new BizException(ErrorCode.BIZ_ERROR.code(), "FAQ 不存在");
        row.setQuestion(normalizeText(question, row.getQuestion(), 500));
        row.setAnswer(normalizeText(answer, row.getAnswer(), 3000));
        row.setScene(normalizeText(scene, "", 32));
        row.setVersionNo((row.getVersionNo() == null ? 1 : row.getVersionNo()) + 1);
        faqMapper.updateById(row);
        return Map.of("id", row.getId(), "question", row.getQuestion(), "answer", row.getAnswer(), "scene", row.getScene() == null ? "" : row.getScene());
    }
    private CustomerServiceMessageEntity insertMessage(CustomerServiceSessionEntity session, String sender, String content, String msgType) {
        if (!StringUtils.hasText(content)) throw new BizException(ErrorCode.BIZ_ERROR.code(), "消息内容不能为空");
        CustomerServiceMessageEntity entity = new CustomerServiceMessageEntity();
        entity.setTenantId(RequestContext.tenantId());
        entity.setSessionId(session.getId());
        entity.setSenderType(sender);
        entity.setSenderId("CUSTOMER".equals(sender) ? session.getCustomerId() : safeOperatorId());
        entity.setContent(content);
        entity.setMsgType(StringUtils.hasText(msgType) ? msgType.trim().toUpperCase() : "TEXT");
        entity.setAiSuggested("AI".equals(sender) ? 1 : 0);
        messageMapper.insert(entity);
        return entity;
    }

    private CustomerServiceSessionEntity mustGetSession(Long id) {
        CustomerServiceSessionEntity row = sessionMapper.selectOne(new LambdaQueryWrapper<CustomerServiceSessionEntity>()
                .eq(CustomerServiceSessionEntity::getTenantId, RequestContext.tenantId())
                .eq(CustomerServiceSessionEntity::getId, id));
        if (row == null) throw new BizException(ErrorCode.BIZ_ERROR.code(), "会话不存在");
        return row;
    }

    private CustomerServiceTicketEntity mustGetTicket(Long id) {
        CustomerServiceTicketEntity row = ticketMapper.selectOne(new LambdaQueryWrapper<CustomerServiceTicketEntity>()
                .eq(CustomerServiceTicketEntity::getTenantId, RequestContext.tenantId())
                .eq(CustomerServiceTicketEntity::getId, id));
        if (row == null) throw new BizException(ErrorCode.BIZ_ERROR.code(), "工单不存在");
        return row;
    }

    private Map<String, Object> toMessageMap(CustomerServiceMessageEntity row) {
        return Map.of("id", row.getId(), "sessionId", row.getSessionId(), "sender", normalizeSender(row.getSenderType()),
                "content", row.getContent(), "msgType", row.getMsgType(), "aiSuggested", row.getAiSuggested(), "createdAt", row.getCreatedAt());
    }

    private Map<String, Object> toTicketMap(CustomerServiceTicketEntity ticket, boolean aiAutoReplied, boolean humanTakenOver, String nextSuggestion) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ticketId", String.valueOf(ticket.getId()));
        map.put("ticketNo", ticket.getTicketNo());
        map.put("sessionId", ticket.getSessionId());
        map.put("priority", normalizePriority(ticket.getPriority()));
        map.put("content", ticket.getContent());
        map.put("status", normalizeTicketStatus(ticket.getStatus(), "OPEN"));
        map.put("assigneeUserId", ticket.getAssigneeUserId());
        map.put("remark", ticket.getRemark() == null ? "" : ticket.getRemark());
        map.put("aiAutoReplied", aiAutoReplied);
        map.put("humanTakenOver", humanTakenOver);
        map.put("nextSuggestion", nextSuggestion);
        map.put("createdAt", ticket.getCreatedAt());
        map.put("dataSource", "MYSQL");
        return map;
    }

    private Map<String, Object> toTimelineMap(BusinessHistoryEventEntity row) {
        return Map.of("occurredAt", row.getOccurredAt(), "operatorId", row.getOperatorId(),
                "operatorName", StringUtils.hasText(row.getOperatorName()) ? row.getOperatorName() : "系统",
                "action", row.getAction(), "actionLabel", row.getActionLabel(),
                "statusFrom", StringUtils.hasText(row.getStatusFrom()) ? row.getStatusFrom() : "-",
                "statusTo", StringUtils.hasText(row.getStatusTo()) ? row.getStatusTo() : "-",
                "note", StringUtils.hasText(row.getNote()) ? row.getNote() : "-");
    }

    private Map<String, Object> buildSuggestion(CustomerServiceSessionEntity session, boolean record) {
        String latestText = latestCustomerText(session.getId());
        List<Map<String, Object>> knowledgeRefs = knowledgeService.matchKnowledge(latestText, "customer-service");
        List<Map<String, Object>> faqHits = knowledgeRefs.stream()
                .filter(ref -> "FAQ".equals(ref.get("type")))
                .map(ref -> {
                    Map<String, Object> hit = new LinkedHashMap<>();
                    hit.put("faqCode", ref.get("code"));
                    hit.put("question", ref.get("title"));
                    hit.put("answer", ref.getOrDefault("answer", ""));
                    hit.put("matchedTags", ref.getOrDefault("matchedTags", List.of()));
                    hit.put("scene", ref.getOrDefault("scene", ""));
                    return hit;
                }).toList();

        String category = suggestCategory(latestText);
        String priority = suggestPriority(session, latestText);
        List<String> knowledgeReplies = faqHits.stream()
                .map(hit -> String.valueOf(hit.get("answer")))
                .filter(StringUtils::hasText)
                .limit(2)
                .toList();
        String autoReplyPriority = knowledgeService.textRule("AUTO_REPLY_PRIORITY", "FAQ,SOP,RULE_PROVIDER");
        String candidatePriority = knowledgeService.textRule("CS_REPLY_CANDIDATE_PRIORITY", "FAQ答案,SOP下一步,标准安抚话术");
        List<String> replies = knowledgeReplies.isEmpty()
                ? List.of("您好，我们已收到您的问题，正在加急处理，稍后同步进展。", "这边已安排专员跟进，预计 30 分钟内反馈结果。", "我们会优先处理并第一时间同步给您。")
                : knowledgeReplies;
        String next = switch (category) {
            case "物流催发" -> "优先确认出库状态并回传物流进度";
            case "库存异常" -> "优先核对库存与库位，必要时转仓储协同";
            case "售后处理" -> "先确认售后信息，再同步预计完成时间";
            default -> "先发送标准回复，再补充责任人和处理备注";
        };

        if (record) {
            recordHistory(RESOURCE_SESSION, String.valueOf(session.getId()), session.getSessionNo(),
                    "AI_SUGGESTION", "AI 建议", session.getStatus(), session.getStatus(), "生成处理建议", null, "AI");
        }

        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("sessionId", session.getId());
        suggestion.put("latestCustomerText", latestText);
        suggestion.put("faqHits", faqHits);
        suggestion.put("replyCandidates", replies);
        suggestion.put("ticketCategorySuggestion", category);
        suggestion.put("prioritySuggestion", priority);
        suggestion.put("sopSuggestion", "核单 -> 确认责任人 -> 更新状态 -> 记录备注 -> 回访确认");
        suggestion.put("basis", knowledgeRefs.isEmpty()
                ? List.of("未命中知识库，已回退规则建议", "结合会话优先级和客户提问")
                : List.of("命中 FAQ/SOP 知识库", "结合会话优先级和客户提问"));
        suggestion.put("knowledgeRefs", knowledgeRefs);
        suggestion.put("knowledgeHit", !knowledgeRefs.isEmpty());
        suggestion.put("knowledgeFallbackNotice", knowledgeRefs.isEmpty() ? "未命中知识库，当前建议来自规则回退。" : "");
        suggestion.put("ruleConfigBasis", Map.of("autoReplyPriority", autoReplyPriority, "candidateReplyPriority", candidatePriority));
        suggestion.put("humanTakeoverSuggested", !hasHistory(RESOURCE_SESSION, String.valueOf(session.getId()), "HUMAN_TAKEOVER") && "HIGH".equals(priority));
        suggestion.put("recommendedAction", next);
        suggestion.put("dataSource", "RULE_PROVIDER");
        return suggestion;
    }

    private String latestCustomerText(Long sessionId) {
        CustomerServiceMessageEntity latest = messageMapper.selectLatestCustomerMessage(RequestContext.tenantId(), sessionId);
        return latest == null ? "客户咨询处理中" : latest.getContent();
    }

    private void recordHistory(String resourceType, String resourceId, String bizNo, String action, String actionLabel,
                               String statusFrom, String statusTo, String note, Long operatorId, String operatorName) {
        BusinessHistoryEventEntity row = new BusinessHistoryEventEntity();
        row.setTenantId(RequestContext.tenantId());
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setBizNo(bizNo);
        row.setAction(action);
        row.setActionLabel(actionLabel);
        row.setStatusFrom(statusFrom);
        row.setStatusTo(statusTo);
        row.setNote(normalizeText(note, "", 500));
        row.setOperatorId(operatorId == null ? safeOperatorId() : operatorId);
        row.setOperatorName(StringUtils.hasText(operatorName) ? operatorName : displayUserName(row.getOperatorId()));
        row.setOccurredAt(LocalDateTime.now());
        historyMapper.insert(row);
    }

    private void recordAudit(String action, String resourceType, String resourceId, String bizNo,
                             String beforeStatus, String afterStatus, String actionLabel, String note) {
        String before = beforeStatus == null ? null : "{\"status\":\"" + safe(beforeStatus) + "\"}";
        String after = "{\"status\":\"" + safe(afterStatus)
                + "\",\"actionLabel\":\"" + safe(actionLabel)
                + "\",\"note\":\"" + safe(note)
                + "\",\"statusFrom\":\"" + safe(beforeStatus)
                + "\",\"statusTo\":\"" + safe(afterStatus) + "\"}";
        auditLogRecordService.record(MODULE, action, resourceType, resourceId, bizNo, before, after);
    }

    private boolean hasHistory(String resourceType, String resourceId, String action) {
        Long count = historyMapper.selectCount(new LambdaQueryWrapper<BusinessHistoryEventEntity>()
                .eq(BusinessHistoryEventEntity::getTenantId, RequestContext.tenantId())
                .eq(BusinessHistoryEventEntity::getResourceType, resourceType)
                .eq(BusinessHistoryEventEntity::getResourceId, resourceId)
                .eq(BusinessHistoryEventEntity::getAction, action));
        return count != null && count > 0;
    }

    private String displayUser(Long userId) {
        if (userId == null) return "未分配";
        return displayUserName(userId) + "(" + userId + ")";
    }

    private String displayUserName(Long userId) {
        if (userId == null) return "系统";
        AuthUserRow row = authQueryMapper.findAuthUserById(RequestContext.tenantId(), userId);
        if (row == null) return "用户#" + userId;
        if (StringUtils.hasText(row.getRealName())) return row.getRealName();
        if (StringUtils.hasText(row.getUsername())) return row.getUsername();
        return "用户#" + userId;
    }

    private String normalizeSenderInput(String senderType, Boolean sendByAi) {
        if (Boolean.TRUE.equals(sendByAi)) return "AI";
        String sender = normalizeText(senderType, "AGENT", 32).toUpperCase();
        return switch (sender) {
            case "CUSTOMER", "AGENT", "AI", "SYSTEM" -> sender;
            default -> "AGENT";
        };
    }

    private String normalizeSender(String senderType) {
        String sender = normalizeText(senderType, "SYSTEM", 32).toUpperCase();
        return switch (sender) {
            case "CUSTOMER", "AGENT", "AI", "SYSTEM" -> sender;
            default -> "SYSTEM";
        };
    }

    private String normalizePriority(String input) {
        String value = normalizeText(input, "MEDIUM", 16).toUpperCase();
        return switch (value) {
            case "LOW", "MEDIUM", "HIGH" -> value;
            default -> "MEDIUM";
        };
    }

    private String normalizeSessionStatus(String input) {
        String value = normalizeText(input, "OPEN", 16).toUpperCase();
        return switch (value) {
            case "OPEN", "PROCESSING", "CLOSED" -> value;
            default -> "OPEN";
        };
    }

    private String normalizeTicketStatus(String input, String fallback) {
        String fb = StringUtils.hasText(fallback) ? fallback.toUpperCase() : "OPEN";
        String value = normalizeText(input, fb, 16).toUpperCase();
        if (!TICKET_STATUS.contains(value)) return fb;
        return value;
    }

    private String suggestCategory(String text) {
        String normalized = normalizeText(text, "", 1000);
        if (containsAny(normalized, "发货", "物流", "快递", "运单")) return "物流催发";
        if (containsAny(normalized, "缺货", "库存", "少货", "超卖")) return "库存异常";
        if (containsAny(normalized, "退货", "换货", "退款", "售后")) return "售后处理";
        return "通用咨询";
    }

    private String suggestPriority(CustomerServiceSessionEntity session, String text) {
        String current = normalizePriority(session.getPriority());
        if ("HIGH".equals(current)) return "HIGH";
        String normalized = normalizeText(text, "", 1000);
        if (containsAny(normalized, "紧急", "马上", "立刻", "投诉", "超时")) return "HIGH";
        if ("LOW".equals(current)) return "LOW";
        return "MEDIUM";
    }

    private boolean containsAny(String source, String... words) {
        if (!StringUtils.hasText(source)) return false;
        for (String word : words) {
            if (source.contains(word)) return true;
        }
        return false;
    }

    private String normalizeText(String input, String fallback, int maxLen) {
        String value = input == null ? "" : input.trim();
        if (!StringUtils.hasText(value)) value = fallback == null ? "" : fallback;
        if (value.length() > maxLen) value = value.substring(0, maxLen);
        return value;
    }

    private Long safeOperatorId() {
        return RequestContext.userId() == null ? 0L : RequestContext.userId();
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
