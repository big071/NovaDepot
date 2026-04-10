package com.novadepot.backend.common.context;

public final class RequestContext {
    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT.set(tenantId);
    }

    public static Long tenantId() {
        return TENANT.get() == null ? 1L : TENANT.get();
    }

    public static void setUserId(Long userId) {
        USER.set(userId);
    }

    public static Long userId() {
        return USER.get();
    }

    public static void clear() {
        TENANT.remove();
        USER.remove();
    }
}
