package com.novadepot.backend.modules.ai;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiStreamRegistry {
    private final Set<String> stopped = ConcurrentHashMap.newKeySet();

    public void stop(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            stopped.add(requestId);
        }
    }

    public boolean isStopped(String requestId) {
        return requestId != null && stopped.contains(requestId);
    }

    public void clear(String requestId) {
        if (requestId != null) {
            stopped.remove(requestId);
        }
    }
}
