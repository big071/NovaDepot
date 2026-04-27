package com.novadepot.backend.modules.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeepSeekChatAiProvider implements AiProvider {
    private final boolean enabled;
    private final String model;

    public DeepSeekChatAiProvider(@Value("${app.ai.deepseek.enabled:false}") boolean enabled,
                                  @Value("${app.ai.deepseek.chat-model:deepseek-chat}") String model) {
        this.enabled = enabled;
        this.model = model;
    }

    @Override
    public String providerName() {
        return "deepseek-chat";
    }

    @Override
    public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
        if (!enabled) {
            throw new IllegalStateException("DeepSeek chat provider is disabled");
        }
        return Map.of(
                "reply", "[DeepSeek-Chat 接入预留] 已接收请求，可在启用真实 API 后返回正式结果。问题：" + message,
                "scene", scene,
                "provider", providerName(),
                "confidence", 0.9,
                "model", model
        );
    }
}
