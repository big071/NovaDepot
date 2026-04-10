package com.novadepot.backend.modules.ai.provider;

import java.util.Map;

public interface AiProvider {
    String providerName();

    default boolean supportsScene(String scene) {
        return true;
    }

    Map<String, Object> chat(String scene, String message, Map<String, Object> context);
}
