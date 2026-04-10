package com.novadepot.backend.modules.ai.provider;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MockAiProvider implements AiProvider {
    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
        return Map.of(
                "reply", "[Mock] 已收到你的问题：" + message,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.86
        );
    }
}
