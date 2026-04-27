package com.novadepot.backend.modules.agent;

import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentTaskExecuteRequest {
    @NotNull(message = "target \u4E0D\u80FD\u4E3A\u7A7A")
    private Map<String, Object> target = new LinkedHashMap<>();

    public Map<String, Object> getTarget() {
        return target;
    }

    public void setTarget(Map<String, Object> target) {
        this.target = target;
    }
}
