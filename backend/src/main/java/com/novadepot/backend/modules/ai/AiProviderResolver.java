package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.modules.ai.provider.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiProviderResolver {
    private final List<AiProvider> providers;
    private final String defaultProvider;
    private final boolean paidEnabled;

    public AiProviderResolver(List<AiProvider> providers,
                              @Value("${app.ai.provider:rule}") String defaultProvider,
                              @Value("${app.ai.paid-enabled:false}") boolean paidEnabled) {
        this.providers = providers;
        this.defaultProvider = defaultProvider;
        this.paidEnabled = paidEnabled;
    }

    public String resolveProviderName(String providerHint) {
        String target = StringUtils.hasText(providerHint) ? providerHint.trim().toLowerCase() : defaultProvider;
        if ("paid".equals(target) && !paidEnabled) {
            return "rule";
        }
        return target;
    }

    public AiProvider resolveProvider(String providerName, String scene) {
        return providers.stream()
                .filter(p -> p.providerName().equalsIgnoreCase(providerName))
                .filter(p -> p.supportsScene(scene))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.BIZ_ERROR.code(), "AI Provider 未配置或不支持当前场景：" + providerName));
    }

    public boolean isDeepSeekProvider(String providerName) {
        return providerName != null && providerName.toLowerCase().startsWith("deepseek");
    }
}
