package com.novadepot.backend.modules.ai.tools;

import java.util.List;
import java.util.Map;

public record AiToolDefinition(
        String name,
        String displayName,
        String description,
        List<String> requiredPermissions,
        Map<String, Object> parameters
) {
    public Map<String, Object> toOpenAiTool() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters
                )
        );
    }
}
