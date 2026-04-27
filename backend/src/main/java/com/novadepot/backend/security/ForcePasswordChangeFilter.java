package com.novadepot.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.repository.AuthQueryMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOW_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/change-password",
            "/api/v1/auth/me"
    );

    private final AuthQueryMapper authQueryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ForcePasswordChangeFilter(AuthQueryMapper authQueryMapper) {
        this.authQueryMapper = authQueryMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/") || ALLOW_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = RequestContext.userId();
        Long tenantId = RequestContext.tenantId();
        if (userId == null || tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Integer force = authQueryMapper.findForcePasswordChange(tenantId, userId);
        if (force != null && force == 1) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error(ErrorCode.FORBIDDEN.code(), "PASSWORD_CHANGE_REQUIRED", null));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
