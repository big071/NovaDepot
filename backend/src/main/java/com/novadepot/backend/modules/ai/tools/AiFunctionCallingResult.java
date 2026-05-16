package com.novadepot.backend.modules.ai.tools;

import java.util.List;
import java.util.Map;

public record AiFunctionCallingResult(
        String reply,
        List<Map<String, Object>> toolCalls,
        List<String> validationWarnings,
        boolean usedTools,
        boolean toolLimitReached
) {
    public static AiFunctionCallingResult empty() {
        return new AiFunctionCallingResult("", List.of(), List.of(), false, false);
    }
}
