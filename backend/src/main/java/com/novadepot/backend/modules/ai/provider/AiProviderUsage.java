package com.novadepot.backend.modules.ai.provider;

import java.math.BigDecimal;

public record AiProviderUsage(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Integer latencyMs,
        BigDecimal costEstimate
) {
    public static AiProviderUsage empty() {
        return new AiProviderUsage(0, 0, 0, 0, BigDecimal.ZERO);
    }
}
