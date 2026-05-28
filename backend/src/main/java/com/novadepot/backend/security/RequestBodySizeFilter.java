package com.novadepot.backend.security;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.common.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestBodySizeFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long maxBodyBytes;

    public RequestBodySizeFilter(ObjectMapper objectMapper,
                                 @Value("${app.security.request-size.enabled:true}") boolean enabled,
                                 @Value("${app.security.request-size.max-body-bytes:10485760}") long maxBodyBytes) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxBodyBytes = Math.max(1, maxBodyBytes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (enabled && contentLength > maxBodyBytes) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<Void> body = ApiResponse.error(
                    ErrorCode.VALIDATION_ERROR.code(),
                    "Request body is too large",
                    MDC.get("traceId")
            );
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
