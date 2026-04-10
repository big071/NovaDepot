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
    public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
        return Map.of(
                "reply", "[PaidProvider Placeholder] " + message,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.92,
                "model", "paid-placeholder"
        );
    }
}
