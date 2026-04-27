package com.novadepot.backend.modules.auth;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.modules.auth.dto.AdminResetPasswordRequest;
import com.novadepot.backend.modules.auth.dto.ChangePasswordRequest;
import com.novadepot.backend.modules.auth.dto.LoginRequest;
import com.novadepot.backend.modules.auth.dto.LoginResponse;
import com.novadepot.backend.security.permission.RequirePermission;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), MDC.get("traceId"));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Boolean>> logout() {
        return ApiResponse.success(Map.of("success", true), MDC.get("traceId"));
    }

    @PostMapping("/change-password")
    @RequirePermission("USER_CHANGE_PASSWORD")
    public ApiResponse<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.success(
                authService.changePassword(RequestContext.tenantId(), RequestContext.userId(),
                        request.getCurrentPassword(), request.getNewPassword()),
                MDC.get("traceId")
        );
    }

    @PostMapping("/users/{id}/reset-password")
    @RequirePermission("USER_RESET_PASSWORD")
    public ApiResponse<Map<String, Object>> resetPassword(@PathVariable Long id,
                                                           @Valid @RequestBody AdminResetPasswordRequest request) {
        return ApiResponse.success(
                authService.resetPasswordByAdmin(RequestContext.tenantId(), RequestContext.userId(), id, request.getNewPassword()),
                MDC.get("traceId")
        );
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth == null ? "anonymous" : auth.getName();
        Long tenantId = RequestContext.tenantId() == null ? 1L : RequestContext.tenantId();
        Long userId = RequestContext.userId() == null ? 1L : RequestContext.userId();
        return ApiResponse.success(authService.meProfile(tenantId, userId, username), MDC.get("traceId"));
    }
}
