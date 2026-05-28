package com.novadepot.backend.security;

import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimitService {
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final int maxFailures;
    private final long windowSeconds;
    private final long lockSeconds;
    private final Clock clock;

    @Autowired
    public LoginRateLimitService(@Value("${app.auth.login.rate-limit.enabled:true}") boolean enabled,
                                 @Value("${app.auth.login.rate-limit.max-failures:10}") int maxFailures,
                                 @Value("${app.auth.login.rate-limit.window-seconds:300}") long windowSeconds,
                                 @Value("${app.auth.login.rate-limit.lock-seconds:300}") long lockSeconds) {
        this(enabled, maxFailures, windowSeconds, lockSeconds, Clock.systemUTC());
    }

    LoginRateLimitService(boolean enabled, int maxFailures, long windowSeconds, long lockSeconds, Clock clock) {
        this.enabled = enabled;
        this.maxFailures = Math.max(1, maxFailures);
        this.windowSeconds = Math.max(1, windowSeconds);
        this.lockSeconds = Math.max(1, lockSeconds);
        this.clock = clock;
    }

    public void checkAllowed(String clientIp, String tenantCode, String username) {
        if (!enabled) {
            return;
        }
        String key = key(clientIp, tenantCode, username);
        AttemptWindow window = attempts.get(key);
        long now = now();
        if (window == null) {
            return;
        }
        if (window.lockedUntilEpochSecond > now) {
            throw new BizException(ErrorCode.FORBIDDEN.code(), "Too many login attempts, please try again later");
        }
        if (window.windowStartedEpochSecond + windowSeconds < now) {
            attempts.remove(key);
        }
    }

    public void recordFailure(String clientIp, String tenantCode, String username) {
        if (!enabled) {
            return;
        }
        String key = key(clientIp, tenantCode, username);
        long now = now();
        attempts.compute(key, (ignored, current) -> {
            AttemptWindow next = current;
            if (next == null || next.windowStartedEpochSecond + windowSeconds < now) {
                next = new AttemptWindow(now, 0, 0);
            }
            int failures = next.failures + 1;
            long lockedUntil = failures >= maxFailures ? now + lockSeconds : next.lockedUntilEpochSecond;
            return new AttemptWindow(next.windowStartedEpochSecond, failures, lockedUntil);
        });
    }

    public void recordSuccess(String clientIp, String tenantCode, String username) {
        if (!enabled) {
            return;
        }
        attempts.remove(key(clientIp, tenantCode, username));
    }

    private String key(String clientIp, String tenantCode, String username) {
        return normalize(clientIp) + "|" + normalize(tenantCode) + "|" + normalize(username);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private long now() {
        return Instant.now(clock).getEpochSecond();
    }

    private record AttemptWindow(long windowStartedEpochSecond, int failures, long lockedUntilEpochSecond) {
    }
}
