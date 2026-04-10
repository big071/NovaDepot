package com.novadepot.backend.modules.auth;

import com.novadepot.backend.modules.auth.dto.LoginRequest;
import com.novadepot.backend.modules.auth.dto.LoginResponse;
import com.novadepot.backend.security.jwt.JwtProperties;
import com.novadepot.backend.security.jwt.JwtTokenService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    public LoginResponse login(LoginRequest request) {
        // scaffold 阶段：固定演示账号，后续替换为 DB 用户校验
        List<String> permissions = List.of(
                "USER_READ", "ROLE_READ", "PERMISSION_READ",
                "PRODUCT_READ", "PRODUCT_CREATE",
                "WAREHOUSE_READ", "WAREHOUSE_CREATE",
                "LOCATION_READ", "LOCATION_CREATE",
                "INVENTORY_READ",
                "INVENTORY_TXN_READ", "INVENTORY_ALERT_READ",
                "INBOUND_READ", "INBOUND_CREATE", "INBOUND_APPROVE", "INBOUND_POST",
                "OUTBOUND_READ", "OUTBOUND_CREATE", "OUTBOUND_APPROVE", "OUTBOUND_SHIP",
                "REPORT_DASHBOARD_READ", "REPORT_INVENTORY_READ",
                "NOTIFY_READ", "AUDIT_READ",
                "AI_CHAT",
                "CS_SESSION_READ", "CS_MESSAGE_SEND", "CS_FAQ_READ", "CS_TICKET_CREATE", "CS_TRANSFER_HUMAN",
                "SETTING_READ"
        );
        String access = jwtTokenService.createToken(1L, 1L, request.getUsername(), permissions);
        return new LoginResponse(access, "mock-refresh-token", jwtProperties.getExpireSeconds());
    }
}
