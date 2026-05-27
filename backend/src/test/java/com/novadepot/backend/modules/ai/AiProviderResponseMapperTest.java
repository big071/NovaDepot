package com.novadepot.backend.modules.ai;

import com.novadepot.backend.modules.ai.provider.AiProviderResponse;
import com.novadepot.backend.modules.ai.provider.AiProviderResponseMapper;
import com.novadepot.backend.modules.ai.provider.AiProviderToolCall;
import com.novadepot.backend.modules.ai.provider.AiProviderUsage;
import com.novadepot.backend.modules.ai.tools.AiToolCall;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderResponseMapperTest {
    private final AiProviderResponseMapper mapper = new AiProviderResponseMapper();

    @Test
    void toExternalMap_shouldKeepCurrentProviderKeys() {
        AiProviderResponse response = AiProviderResponse.builder("enterprise", "deepseek-chat")
                .reply("ok")
                .model("deepseek-chat")
                .confidence(BigDecimal.valueOf(0.88))
                .tokens(12)
                .usage(new AiProviderUsage(3, 9, 12, 25, BigDecimal.valueOf(0.000012)))
                .toolCalls(List.of(AiProviderToolCall.request("query_inventory", "{\"limit\":10}")))
                .metrics(Map.of("inventoryPoints", 2))
                .suggestions(List.of(Map.of("productId", 1L)))
                .metadata(Map.of("fallbackReason", "test"))
                .build();

        Map<String, Object> external = mapper.toExternalMap(response);

        assertThat(external).containsEntry("reply", "ok")
                .containsEntry("scene", "enterprise")
                .containsEntry("provider", "deepseek-chat")
                .containsEntry("model", "deepseek-chat")
                .containsEntry("tokens", 12)
                .containsEntry("fallbackReason", "test");
        assertThat(external.get("usage")).isInstanceOf(Map.class);
        assertThat(external.get("toolCalls")).isInstanceOf(List.class);
        assertThat(external.get("metrics")).isEqualTo(Map.of("inventoryPoints", 2));
        assertThat(external.get("suggestions")).isEqualTo(List.of(Map.of("productId", 1L)));
    }

    @Test
    void toolCallsToFunctionCalls_shouldMapProviderToolCalls() {
        AiProviderResponse response = AiProviderResponse.builder("enterprise", "deepseek-chat")
                .toolCalls(List.of(AiProviderToolCall.request("query_inventory", "{\"lowStock\":true}")))
                .build();

        List<AiToolCall> calls = mapper.toolCallsToFunctionCalls(response);

        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).toolName()).isEqualTo("query_inventory");
        assertThat(calls.get(0).argumentsJson()).isEqualTo("{\"lowStock\":true}");
    }
}
