package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.modules.ai.provider.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderResolverTest {

    @Test
    void resolveProviderName_usesDefaultAndHint() {
        AiProviderResolver resolver = new AiProviderResolver(List.of(provider("rule"), provider("mock")), "deepseek-chat", false);

        assertThat(resolver.resolveProviderName(null)).isEqualTo("deepseek-chat");
        assertThat(resolver.resolveProviderName(" RULE ")).isEqualTo("rule");
    }

    @Test
    void resolveProviderName_paidDisabledFallsBackToRule() {
        AiProviderResolver resolver = new AiProviderResolver(List.of(provider("rule"), provider("paid")), "paid", false);

        assertThat(resolver.resolveProviderName(null)).isEqualTo("rule");
        assertThat(resolver.resolveProviderName("paid")).isEqualTo("rule");
    }

    @Test
    void resolveProvider_findsProviderByNameAndScene() {
        AiProvider warehouseOnly = new AiProvider() {
            @Override
            public String providerName() {
                return "deepseek-chat";
            }

            @Override
            public boolean supportsScene(String scene) {
                return "warehouse".equals(scene);
            }

            @Override
            public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
                return Map.of();
            }
        };
        AiProviderResolver resolver = new AiProviderResolver(List.of(warehouseOnly), "deepseek-chat", true);

        assertThat(resolver.resolveProvider("deepseek-chat", "warehouse")).isSameAs(warehouseOnly);
        assertThatThrownBy(() -> resolver.resolveProvider("deepseek-chat", "enterprise"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("AI Provider 未配置或不支持当前场景：deepseek-chat");
    }

    @Test
    void isDeepSeekProvider_detectsDeepSeekNamesOnly() {
        AiProviderResolver resolver = new AiProviderResolver(List.of(), "rule", false);

        assertThat(resolver.isDeepSeekProvider("deepseek-chat")).isTrue();
        assertThat(resolver.isDeepSeekProvider("deepseek-reasoner")).isTrue();
        assertThat(resolver.isDeepSeekProvider("rule")).isFalse();
        assertThat(resolver.isDeepSeekProvider(null)).isFalse();
    }

    private AiProvider provider(String name) {
        return new AiProvider() {
            @Override
            public String providerName() {
                return name;
            }

            @Override
            public Map<String, Object> chat(String scene, String message, Map<String, Object> context) {
                return Map.of();
            }
        };
    }
}
