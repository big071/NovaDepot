package com.novadepot.backend.modules.ai.provider;

public record AiProviderError(
        String code,
        String message,
        Integer statusCode,
        boolean retryable
) {
}
