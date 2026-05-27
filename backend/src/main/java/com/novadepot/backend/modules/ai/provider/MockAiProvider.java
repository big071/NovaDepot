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
    public AiProviderResponse chat(String scene, String message, Map<String, Object> context) {
        return AiProviderResponse.builder(scene, providerName())
                .reply("[Mock] 已收到你的问题：" + message)
                .confidence(0.86)
                .build();
    }
}
