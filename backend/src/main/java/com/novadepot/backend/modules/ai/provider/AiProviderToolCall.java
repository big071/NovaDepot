package com.novadepot.backend.modules.ai.provider;

public record AiProviderToolCall(
        String id,
        String name,
        String argumentsJson,
        String resultSummary,
        boolean success,
        String errorCode
) {
    public static AiProviderToolCall request(String name, String argumentsJson) {
        return new AiProviderToolCall(null, name, argumentsJson, null, true, null);
    }
}
