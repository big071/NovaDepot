package com.novadepot.backend.modules.customerservice;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CustomerServiceService {
    public List<Map<String, Object>> sessions() {
        return List.of(
                Map.of("id", 1, "sessionNo", "CS-20260410-001", "status", "OPEN", "priority", "HIGH"),
                Map.of("id", 2, "sessionNo", "CS-20260410-002", "status", "PROCESSING", "priority", "MEDIUM")
        );
    }

    public List<Map<String, Object>> messages(Long sessionId) {
        return List.of(
                Map.of("id", 1, "sessionId", sessionId, "sender", "CUSTOMER", "content", "请问订单什么时候发货？", "msgType", "TEXT"),
                Map.of("id", 2, "sessionId", sessionId, "sender", "AI", "content", "建议回复：预计今日18:00前发货", "msgType", "AI_SUGGESTION")
        );
    }

    public Map<String, Object> sendMessage(Long sessionId, String content, String msgType, Boolean sendByAi) {
        boolean ai = Boolean.TRUE.equals(sendByAi);
        return Map.of(
                "sessionId", sessionId,
                "messageId", Math.abs((sessionId + content).hashCode()),
                "sender", ai ? "AI" : "AGENT",
                "msgType", msgType == null || msgType.isBlank() ? "TEXT" : msgType,
                "content", content,
                "createdAt", LocalDateTime.now()
        );
    }

    public Map<String, Object> transferHuman(Long sessionId, Long targetUserId) {
        return Map.of("sessionId", sessionId, "assignedUserId", targetUserId, "status", "TRANSFERRED");
    }

    public Map<String, Object> createTicket(Long sessionId, String priority, String content) {
        String ticketNo = "TCK-" + System.currentTimeMillis();
        return Map.of(
                "ticketId", Math.abs((ticketNo + sessionId).hashCode()),
                "ticketNo", ticketNo,
                "sessionId", sessionId,
                "priority", priority == null || priority.isBlank() ? "MEDIUM" : priority,
                "content", content,
                "status", "OPEN"
        );
    }

    public List<Map<String, Object>> faq(String keyword, String scene) {
        List<Map<String, Object>> all = List.of(
                Map.of("id", 1, "question", "订单什么时候发货？", "answer", "通常24小时内发货，节假日顺延。", "scene", "shipping"),
                Map.of("id", 2, "question", "如何申请退换货？", "answer", "请在签收后7日内提交售后申请并上传凭证。", "scene", "after-sale"),
                Map.of("id", 3, "question", "库存不足怎么办？", "answer", "系统会提示低库存预警并给出补货建议。", "scene", "inventory")
        );
        return all.stream()
                .filter(x -> keyword == null || keyword.isBlank() || String.valueOf(x.get("question")).contains(keyword))
                .filter(x -> scene == null || scene.isBlank() || String.valueOf(x.get("scene")).equalsIgnoreCase(scene))
                .toList();
    }
}
