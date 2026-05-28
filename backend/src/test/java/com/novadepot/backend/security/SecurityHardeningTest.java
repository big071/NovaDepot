package com.novadepot.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.common.exception.GlobalExceptionHandler;
import com.novadepot.backend.common.utils.SensitiveDataMasker;
import com.novadepot.backend.security.jwt.JwtAuthFilter;
import com.novadepot.backend.security.jwt.JwtProperties;
import com.novadepot.backend.security.jwt.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityHardeningTest {

    @Test
    void jwt_shouldRejectShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short");
        properties.setExpireSeconds(7200);
        JwtTokenService service = new JwtTokenService(properties, new MockEnvironment().withProperty("spring.profiles.active", "test"));

        assertThatThrownBy(() -> service.createToken(1L, 1L, "admin", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void jwt_shouldCreateAndParseTokenWithStrongSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test_only_jwt_secret_32_chars_minimum");
        properties.setExpireSeconds(7200);
        JwtTokenService service = new JwtTokenService(properties, new MockEnvironment().withProperty("spring.profiles.active", "test"));

        String token = service.createToken(1L, 1L, "admin", List.of("REPORT_VIEW"));

        assertThat(service.parse(token).getSubject()).isEqualTo("admin");
    }

    @Test
    void loginRateLimit_shouldLockByIpTenantAndUsername() {
        LoginRateLimitService limiter = new LoginRateLimitService(true, 2, 300, 300, java.time.Clock.systemUTC());

        limiter.checkAllowed("127.0.0.1", "default", "admin");
        limiter.recordFailure("127.0.0.1", "default", "admin");
        limiter.checkAllowed("127.0.0.1", "default", "admin");
        limiter.recordFailure("127.0.0.1", "default", "admin");

        assertThatThrownBy(() -> limiter.checkAllowed("127.0.0.1", "default", "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Too many login attempts");
        limiter.checkAllowed("127.0.0.2", "default", "admin");
    }

    @Test
    void requestBodySizeFilter_shouldRejectOversizedContentLength() throws Exception {
        RequestBodySizeFilter filter = new RequestBodySizeFilter(new ObjectMapper().findAndRegisterModules(), true, 10);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContent(new byte[11]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(response.getContentAsString()).contains(ErrorCode.VALIDATION_ERROR.code());
        assertThat(response.getContentAsString()).contains("Request body is too large");
    }

    @Test
    void cors_shouldAllowConfiguredDevOriginAndRejectWildcard() {
        SecurityConfig config = new SecurityConfig(
                mock(JwtAuthFilter.class),
                mock(ForcePasswordChangeFilter.class),
                mock(RestAuthHandlers.class),
                "http://localhost:3100,http://127.0.0.1:5173"
        );

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).contains("http://localhost:3100");

        SecurityConfig unsafe = new SecurityConfig(
                mock(JwtAuthFilter.class),
                mock(ForcePasswordChangeFilter.class),
                mock(RestAuthHandlers.class),
                "*"
        );
        assertThatThrownBy(unsafe::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wildcard");
    }

    @Test
    void sensitiveDataMasker_shouldMaskKeysTokensAndPasswords() {
        String raw = "apiKey=sk-1234567890abcdef password=secret123 token=eyJabcdefghij.abcdefghijklmnop.qrstuvwxyz123";

        String masked = SensitiveDataMasker.mask(raw);

        assertThat(masked).doesNotContain("sk-1234567890abcdef");
        assertThat(masked).doesNotContain("secret123");
        assertThat(masked).doesNotContain("eyJabcdefghij.abcdefghijklmnop.qrstuvwxyz123");
        assertThat(masked).contains("****");
    }

    @Test
    void globalExceptionHandler_shouldMaskSecretInBizExceptionResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleBiz(new BizException(ErrorCode.BIZ_ERROR.code(), "apiKey=sk-1234567890abcdef"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("sk-1234567890abcdef");
        assertThat(response.getBody().message()).contains("****");
    }
}
