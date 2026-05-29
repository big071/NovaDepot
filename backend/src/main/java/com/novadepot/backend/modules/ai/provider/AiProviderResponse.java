package com.novadepot.backend.modules.ai.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AiProviderResponse(
        String reply,
        String scene,
        String provider,
        String model,
        BigDecimal confidence,
        Integer tokens,
        boolean success,
        String errorCode,
        String errorMessage,
        AiProviderUsage usage,
        List<AiProviderToolCall> toolCalls,
        Map<String, Object> metrics,
        List<Map<String, Object>> suggestions,
        Map<String, Object> metadata
) {
    public AiProviderResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static Builder builder(String scene, String provider) {
        return new Builder(scene, provider);
    }

    public static class Builder {
        private String reply = "";
        private final String scene;
        private final String provider;
        private String model;
        private BigDecimal confidence;
        private Integer tokens;
        private boolean success = true;
        private String errorCode;
        private String errorMessage;
        private AiProviderUsage usage;
        private List<AiProviderToolCall> toolCalls = List.of();
        private Map<String, Object> metrics = Map.of();
        private List<Map<String, Object>> suggestions = List.of();
        private Map<String, Object> metadata = Map.of();

        private Builder(String scene, String provider) {
            this.scene = scene;
            this.provider = provider;
        }

        public Builder reply(String reply) {
            this.reply = reply;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder confidence(BigDecimal confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder confidence(Number confidence) {
            this.confidence = confidence == null ? null : BigDecimal.valueOf(confidence.doubleValue());
            return this;
        }

        public Builder tokens(Integer tokens) {
            this.tokens = tokens;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder error(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.success = false;
            return this;
        }

        public Builder usage(AiProviderUsage usage) {
            this.usage = usage;
            return this;
        }

        public Builder toolCalls(List<AiProviderToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public Builder metrics(Map<String, Object> metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder suggestions(List<Map<String, Object>> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public AiProviderResponse build() {
            return new AiProviderResponse(reply, scene, provider, model, confidence, tokens, success,
                    errorCode, errorMessage, usage, toolCalls, metrics, suggestions, metadata);
        }
    }
}
