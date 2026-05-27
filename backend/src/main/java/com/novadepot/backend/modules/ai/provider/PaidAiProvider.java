package com.novadepot.backend.modules.ai.provider;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaidAiProvider implements AiProvider {
    @Override
    public String providerName() {
        return "paid";
    }

    @Override
    public AiProviderResponse chat(String scene, String message, Map<String, Object> context) {
        return AiProviderResponse.builder(scene, providerName())
                .reply("[PaidProvider Placeholder] " + message)
                .confidence(0.92)
                .model("paid-placeholder")
                .build();
    }
}
