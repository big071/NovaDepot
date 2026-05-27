package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.config.AiProperties;
import com.novadepot.backend.model.entity.AiUsageLogEntity;
import com.novadepot.backend.modules.ai.provider.AiProviderCallException;
import com.novadepot.backend.modules.ai.provider.AiProviderResponse;
import com.novadepot.backend.modules.ai.provider.DeepSeekChatAiProvider;
import com.novadepot.backend.modules.ai.provider.MockAiProvider;
import com.novadepot.backend.modules.ai.provider.PaidAiProvider;
import com.novadepot.backend.repository.AiUsageLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiProviderTypeSafetyTest {
    @Test
    void deepSeekChatProvider_shouldMapReplyUsageAndToolCalls() {
        AiProperties properties = new AiProperties();
        properties.getDeepseek().setEnabled(true);
        properties.getDeepseek().setApiKey("test-key");
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.deepseek.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        AiUsageLogMapper usageLogMapper = mock(AiUsageLogMapper.class);
        server.expect(requestTo("https://api.deepseek.com/v1/chat/completions"))
                .andRespond(withSuccess("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "typed reply",
                        "tool_calls": [
                          {"function": {"name": "query_inventory", "arguments": "{\\"limit\\":10}"}}
                        ]
                      }
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 7,
                    "completion_tokens": 11,
                    "total_tokens": 18
                  }
                }
                """, MediaType.APPLICATION_JSON));

        DeepSeekChatAiProvider provider = new DeepSeekChatAiProvider(
                properties, restClient, usageLogMapper, new ObjectMapper());

        AiProviderResponse response = provider.chat("enterprise", "hello", Map.of());

        assertThat(response.reply()).isEqualTo("typed reply");
        assertThat(response.model()).isEqualTo(properties.getDeepseekChatModel());
        assertThat(response.tokens()).isEqualTo(18);
        assertThat(response.usage().promptTokens()).isEqualTo(7);
        assertThat(response.usage().completionTokens()).isEqualTo(11);
        assertThat(response.usage().totalTokens()).isEqualTo(18);
        assertThat(response.toolCalls()).hasSize(1);
        assertThat(response.toolCalls().get(0).name()).isEqualTo("query_inventory");
        assertThat(response.toolCalls().get(0).argumentsJson()).isEqualTo("{\"limit\":10}");
        verify(usageLogMapper).insert(any(AiUsageLogEntity.class));
        server.verify();
    }

    @Test
    void mockProvider_shouldReturnTypedResponse() {
        AiProviderResponse response = new MockAiProvider().chat("enterprise", "hello", Map.of());

        assertThat(response.provider()).isEqualTo("mock");
        assertThat(response.scene()).isEqualTo("enterprise");
        assertThat(response.reply()).contains("hello");
        assertThat(response.confidence()).isNotNull();
    }

    @Test
    void paidProvider_shouldReturnTypedResponse() {
        AiProviderResponse response = new PaidAiProvider().chat("enterprise", "hello", Map.of());

        assertThat(response.provider()).isEqualTo("paid");
        assertThat(response.model()).isEqualTo("paid-placeholder");
        assertThat(response.reply()).contains("hello");
    }

    @Test
    void deepSeekDisabled_shouldThrowProviderCallExceptionWithoutCallingApi() {
        AiProperties properties = new AiProperties();
        properties.getDeepseek().setEnabled(false);
        RestClient restClient = mock(RestClient.class);
        AiUsageLogMapper usageLogMapper = mock(AiUsageLogMapper.class);
        DeepSeekChatAiProvider provider = new DeepSeekChatAiProvider(
                properties, restClient, usageLogMapper, new ObjectMapper());

        assertThatThrownBy(() -> provider.chat("enterprise", "hello", Map.of()))
                .isInstanceOf(AiProviderCallException.class)
                .satisfies(error -> {
                    AiProviderCallException ex = (AiProviderCallException) error;
                    assertThat(ex.getProvider()).isEqualTo("deepseek-chat");
                    assertThat(ex.getModel()).isEqualTo(properties.getDeepseekChatModel());
                    assertThat(ex.getErrorCode()).isEqualTo("DEEPSEEK_FAILED");
                });
        verify(restClient, never()).post();
        verify(usageLogMapper).insert(any(AiUsageLogEntity.class));
    }
}
