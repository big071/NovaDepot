package com.novadepot.backend.modules.settings;

import com.novadepot.backend.common.context.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SettingsService {
    private final String aiProvider;
    private final String tenantMode;
    private final boolean billingEnabled;
    private final boolean planEnforceEnabled;

    public SettingsService(@Value("${app.ai.provider:rule}") String aiProvider,
                           @Value("${app.tenant.mode:single}") String tenantMode,
                           @Value("${app.billing.enabled:false}") boolean billingEnabled,
                           @Value("${app.plan-enforce-enabled:false}") boolean planEnforceEnabled) {
        this.aiProvider = aiProvider;
        this.tenantMode = tenantMode;
        this.billingEnabled = billingEnabled;
        this.planEnforceEnabled = planEnforceEnabled;
    }

    public Map<String, Object> getSettings() {
        return Map.of(
                "appName", "NovaDepot",
                "tenantId", RequestContext.tenantId(),
                "aiProvider", aiProvider,
                "tenantMode", tenantMode,
                "billingEnabled", billingEnabled,
                "planEnforceEnabled", planEnforceEnabled
        );
    }
}
