package com.novadepot.backend.modules.auth;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.modules.auth.dto.LoginRequest;
import com.novadepot.backend.modules.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
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

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.success(Map.of("userId", 1, "username", "admin", "tenantId", 1), MDC.get("traceId"));
    }
}
