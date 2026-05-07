package com.novadepot.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private final AiProperties aiProperties;

    public RestClientConfig(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        Duration connect = aiProperties.getConnectTimeout();
        Duration read = aiProperties.getReadTimeout();

        return RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                        java.net.http.HttpClient.newBuilder()
                                .connectTimeout(connect)
                                .build()))
                .defaultHeader("Content-Type", "application/json");
    }

    @Bean
    public RestClient aiRestClient(RestClient.Builder builder) {
        RestClient.Builder configured = builder.baseUrl(aiProperties.getDeepseek().getBaseUrl());
        if (StringUtils.hasText(aiProperties.getDeepseek().getApiKey())) {
            configured.defaultHeader("Authorization", "Bearer " + aiProperties.getDeepseek().getApiKey());
        }
        return configured.build();
    }
}
