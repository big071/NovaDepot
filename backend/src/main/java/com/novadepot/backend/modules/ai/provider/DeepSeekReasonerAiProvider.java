package com.novadepot.backend.modules.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeepSeekReasonerAiProvider implements AiProvider {
    private final boolean enabled;
    private final String model;

    public DeepSeekReasonerAiProvider(@Value("${app.ai.deepseek.enabled:false}") boolean enabled,
                                      @Value("${app.ai.deepseek.reasoner-model:deepseek-reasoner}") String model) {
        this.enabled = enabled;
        this.model = model;
    }

    @Override
    public String providerName() {
        return "deepseek-reasoner";
    }

    @Override
    public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
        if (!enabled) {
            throw new IllegalStateException("DeepSeek reasoner provider is disabled");
        }
        return Map.of(
                "reply", "[DeepSeek-Reasoner 接入预留] 已接收复杂推理请求，可在启用真实 API 后返回正式结果。问题：" + message,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.92,
                "model", model
        );
    }
}
