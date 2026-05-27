package com.novadepot.backend.modules.ai.provider;

import com.novadepot.backend.modules.ai.tools.AiToolCall;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiProviderResponseMapper {
    public Map<String, Object> toExternalMap(AiProviderResponse response) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reply", response.reply());
        map.put("scene", response.scene());
        map.put("provider", response.provider());
        if (response.confidence() != null) {
            map.put("confidence", response.confidence());
        }
        if (response.model() != null) {
            map.put("model", response.model());
        }
        if (response.tokens() != null) {
            map.put("tokens", response.tokens());
        }
        if (response.usage() != null) {
            map.put("usage", usageToMap(response.usage()));
        }
        if (!response.toolCalls().isEmpty()) {
            map.put("toolCalls", providerToolCallsToMap(response.toolCalls()));
        }
        if (!response.metrics().isEmpty()) {
            map.put("metrics", response.metrics());
        }
        if (!response.suggestions().isEmpty()) {
            map.put("suggestions", response.suggestions());
        }
        if (!response.metadata().isEmpty()) {
            map.putAll(response.metadata());
        }
        if (!response.success()) {
            map.put("success", false);
            map.put("errorCode", response.errorCode());
            map.put("errorMessage", response.errorMessage());
        }
        return map;
    }

    public List<AiToolCall> toolCallsToFunctionCalls(AiProviderResponse response) {
        return response.toolCalls().stream()
                .filter(call -> call.name() != null && !call.name().isBlank())
                .map(call -> new AiToolCall(call.name(), call.argumentsJson() == null ? "{}" : call.argumentsJson()))
                .toList();
    }

    public Map<String, Object> usageToMap(AiProviderUsage usage) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("promptTokens", usage.promptTokens());
        map.put("completionTokens", usage.completionTokens());
        map.put("totalTokens", usage.totalTokens());
        map.put("latencyMs", usage.latencyMs());
        map.put("costEstimate", usage.costEstimate());
        return map;
    }

    private List<Map<String, Object>> providerToolCallsToMap(List<AiProviderToolCall> toolCalls) {
        return toolCalls.stream()
                .map(call -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    if (call.id() != null) {
                        map.put("id", call.id());
                    }
                    map.put("name", call.name());
                    map.put("arguments", call.argumentsJson() == null ? "{}" : call.argumentsJson());
                    if (call.resultSummary() != null) {
                        map.put("resultSummary", call.resultSummary());
                    }
                    map.put("success", call.success());
                    if (call.errorCode() != null) {
                        map.put("errorCode", call.errorCode());
                    }
                    return map;
                })
                .toList();
    }
}
