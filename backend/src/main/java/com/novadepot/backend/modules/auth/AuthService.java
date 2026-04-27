package com.novadepot.backend.modules.auth;

import com.novadepot.backend.common.enums.ErrorCode;
import com.novadepot.backend.common.exception.BizException;
import com.novadepot.backend.modules.auditlogs.AuditLogRecordService;
import com.novadepot.backend.modules.auth.dto.LoginRequest;
import com.novadepot.backend.modules.auth.dto.LoginResponse;
import com.novadepot.backend.repository.AuthQueryMapper;
import com.novadepot.backend.security.jwt.JwtProperties;
import com.novadepot.backend.security.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class AuthService {
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AuthQueryMapper authQueryMapper;
    private final AuditLogRecordService auditLogRecordService;
    private final int passwordMinLength;
    private final boolean enforcePasswordStrengthOnLogin;
    private final boolean passwordRequireUppercase;
    private final boolean passwordRequireLowercase;
    private final boolean passwordRequireDigit;
    private final boolean passwordRequireSpecial;
    private final boolean passwordDenyUsernameContains;
    private final int loginFailMaxAttempts;
    private final int loginFailLockMinutes;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties,
                       AuthQueryMapper authQueryMapper,
                       AuditLogRecordService auditLogRecordService,
                       @Value("${app.auth.password.min-length:8}") int passwordMinLength,
                       @Value("${app.auth.password.enforce-strength-on-login:false}") boolean enforcePasswordStrengthOnLogin,
                       @Value("${app.auth.password.require-uppercase:true}") boolean passwordRequireUppercase,
                       @Value("${app.auth.password.require-lowercase:true}") boolean passwordRequireLowercase,
                       @Value("${app.auth.password.require-digit:true}") boolean passwordRequireDigit,
                       @Value("${app.auth.password.require-special:false}") boolean passwordRequireSpecial,
                       @Value("${app.auth.password.deny-username-contains:true}") boolean passwordDenyUsernameContains,
                       @Value("${app.auth.login.fail-max-attempts:5}") int loginFailMaxAttempts,
                       @Value("${app.auth.login.lock-minutes:15}") int loginFailLockMinutes) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.authQueryMapper = authQueryMapper;
        this.auditLogRecordService = auditLogRecordService;
        this.passwordMinLength = passwordMinLength;
        this.enforcePasswordStrengthOnLogin = enforcePasswordStrengthOnLogin;
        this.passwordRequireUppercase = passwordRequireUppercase;
        this.passwordRequireLowercase = passwordRequireLowercase;
        this.passwordRequireDigit = passwordRequireDigit;
        this.passwordRequireSpecial = passwordRequireSpecial;
        this.passwordDenyUsernameContains = passwordDenyUsernameContains;
        this.loginFailMaxAttempts = Math.max(1, loginFailMaxAttempts);
        this.loginFailLockMinutes = Math.max(1, loginFailLockMinutes);
    }

    public LoginResponse login(LoginRequest request) {
        validatePasswordPolicyForLogin(request);

        AuthUserRow user = authQueryMapper.findAuthUser(request.getTenantCode(), request.getUsername());
        if (user == null) {
            auditLogRecordService.record("AUTH", "LOGIN_FAIL", "USER", request.getUsername(), null, null,
                    "{\"reason\":\"USER_NOT_FOUND\"}");
            throw new BizException(ErrorCode.UNAUTHORIZED.code(), "Invalid username or password");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getTenantStatus()) || !"ACTIVE".equalsIgnoreCase(user.getUserStatus())) {
            auditLogRecordService.record("AUTH", "LOGIN_FAIL", "USER", String.valueOf(user.getUserId()), null, null,
                    "{\"reason\":\"ACCOUNT_NOT_ACTIVE\"}");
            throw new BizException(ErrorCode.FORBIDDEN.code(), "Account is not active");
        }

        if (isLocked(user)) {
            auditLogRecordService.record("AUTH", "LOGIN_FAIL", "USER", String.valueOf(user.getUserId()), null, null,
                    "{\"reason\":\"ACCOUNT_LOCKED\",\"lockUntil\":\"" + user.getLockUntil() + "\"}");
            throw new BizException(ErrorCode.FORBIDDEN.code(), "Account is temporarily locked");
        }

        if (!matchesPassword(request.getPassword(), user.getPasswordHash(), user.getTenantId(), user.getUserId())) {
            handleLoginFail(user);
            throw new BizException(ErrorCode.UNAUTHORIZED.code(), "Invalid username or password");
        }

        authQueryMapper.markLoginSuccess(user.getTenantId(), user.getUserId());

        List<String> permissions = authQueryMapper.findPermissions(user.getTenantId(), user.getUserId());
        if (permissions == null) {
            permissions = Collections.emptyList();
        }

        String access = jwtTokenService.createToken(user.getUserId(), user.getTenantId(), user.getUsername(), permissions);
        boolean mustChangePassword = user.getForcePasswordChange() != null && user.getForcePasswordChange() == 1;
        auditLogRecordService.record("AUTH", "LOGIN_SUCCESS", "USER", String.valueOf(user.getUserId()), null, null,
                "{\"username\":\"" + user.getUsername() + "\",\"mustChangePassword\":" + mustChangePassword + "}");
        return new LoginResponse(access, "mock-refresh-token", jwtProperties.getExpireSeconds(), mustChangePassword);
    }

    public Map<String, Object> changePassword(Long tenantId, Long userId, String currentPassword, String newPassword) {
        AuthUserRow user = authQueryMapper.findAuthUserById(tenantId, userId);
        if (user == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "User not found");
        }
        if (!matchesPassword(currentPassword, user.getPasswordHash(), tenantId, userId)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Current password is incorrect");
        }
        validatePasswordStrength(newPassword, user.getUsername());
        String hash = passwordEncoder.encode(newPassword);
        authQueryMapper.changePassword(tenantId, userId, hash);
        auditLogRecordService.record("AUTH", "CHANGE_PASSWORD", "USER", String.valueOf(userId), null,
                "{\"forcePasswordChange\":" + (user.getForcePasswordChange() == null ? 0 : user.getForcePasswordChange()) + "}",
                "{\"forcePasswordChange\":0}");
        return Map.of("success", true);
    }

    public Map<String, Object> resetPasswordByAdmin(Long tenantId, Long operatorUserId, Long targetUserId, String newPassword) {
        if (!isTenantAdmin(tenantId, operatorUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN.code(), "Only admin can reset other users password");
        }
        AuthUserRow target = authQueryMapper.findAuthUserById(tenantId, targetUserId);
        if (target == null) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "User not found");
        }
        validatePasswordStrength(newPassword, target.getUsername());
        String hash = passwordEncoder.encode(newPassword);
        authQueryMapper.resetPasswordByAdmin(tenantId, targetUserId, hash);
        auditLogRecordService.record("AUTH", "RESET_PASSWORD", "USER", String.valueOf(targetUserId), null,
                "{\"forcePasswordChange\":" + (target.getForcePasswordChange() == null ? 0 : target.getForcePasswordChange()) + "}",
                "{\"forcePasswordChange\":1}");
        return Map.of("success", true, "targetUserId", targetUserId, "forcePasswordChange", true);
    }

    public Map<String, Object> meProfile(Long tenantId, Long userId, String username) {
        AuthUserRow user = authQueryMapper.findAuthUserById(tenantId, userId);
        List<String> permissions = authQueryMapper.findPermissions(tenantId, userId);
        List<String> roleCodes = authQueryMapper.findRoleCodes(tenantId, userId);

        List<String> safePermissions = permissions == null ? List.of() : permissions;
        List<String> safeRoleCodes = roleCodes == null ? List.of() : roleCodes;
        String roleKey = resolveRoleKey(safeRoleCodes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("username", username);
        result.put("realName", user == null ? username : user.getRealName());
        result.put("tenantId", tenantId);
        result.put("roleCodes", safeRoleCodes);
        result.put("permissions", safePermissions);
        result.put("roleKey", roleKey);
        result.put("roleNameZh", roleNameZh(roleKey));
        result.put("mustChangePassword", user != null && user.getForcePasswordChange() != null && user.getForcePasswordChange() == 1);
        return result;
    }

    private void validatePasswordPolicyForLogin(LoginRequest request) {
        if (!enforcePasswordStrengthOnLogin) {
            return;
        }
        validatePasswordStrength(request.getPassword(), request.getUsername());
    }

    private void validatePasswordStrength(String password, String username) {
        if (password == null || password.length() < passwordMinLength) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Password does not meet policy");
        }
        if (passwordRequireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Password does not meet policy");
        }
        if (passwordRequireLowercase && password.chars().noneMatch(Character::isLowerCase)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Password does not meet policy");
        }
        if (passwordRequireDigit && password.chars().noneMatch(Character::isDigit)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Password does not meet policy");
        }
        if (passwordRequireSpecial && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Password does not meet policy");
        }
        if (passwordDenyUsernameContains && username != null && !username.isBlank()
                && password.toLowerCase().contains(username.toLowerCase())) {
            throw new BizException(ErrorCode.BIZ_ERROR.code(), "Password does not meet policy");
        }
    }

    private void handleLoginFail(AuthUserRow user) {
        int current = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
        int next = current + 1;
        LocalDateTime lockUntil = null;
        if (next >= loginFailMaxAttempts) {
            lockUntil = LocalDateTime.now().plusMinutes(loginFailLockMinutes);
            next = 0;
        }
        authQueryMapper.updateLoginSecurity(user.getTenantId(), user.getUserId(), next, lockUntil);
        String afterJson = lockUntil == null
                ? "{\"reason\":\"PASSWORD_MISMATCH\",\"failedLoginCount\":" + next + "}"
                : "{\"reason\":\"PASSWORD_MISMATCH\",\"failedLoginCount\":0,\"lockUntil\":\"" + lockUntil + "\"}";
        auditLogRecordService.record("AUTH", "LOGIN_FAIL", "USER", String.valueOf(user.getUserId()), null, null, afterJson);
    }

    private boolean isLocked(AuthUserRow user) {
        return user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now());
    }

    private boolean matchesPassword(String rawPassword, String dbPassword, Long tenantId, Long userId) {
        if (dbPassword == null || dbPassword.isBlank()) {
            return false;
        }
        try {
            if (dbPassword.startsWith("$2a$") || dbPassword.startsWith("$2b$") || dbPassword.startsWith("$2y$")) {
                return passwordEncoder.matches(rawPassword, dbPassword);
            }
            boolean ok = rawPassword.equals(dbPassword);
            if (ok) {
                String upgraded = passwordEncoder.encode(rawPassword);
                authQueryMapper.updatePasswordHash(tenantId, userId, upgraded);
            }
            return ok;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String resolveRoleKey(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return "observer";
        }
        List<String> normalized = roleCodes.stream()
                .map(code -> code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (normalized.contains("TENANT_ADMIN")) {
            return "admin";
        }
        if (normalized.contains("WAREHOUSE_MANAGER") || normalized.contains("WAREHOUSE_OPERATOR")) {
            return "warehouse_ops";
        }
        if (normalized.contains("CS_AGENT")) {
            return "cs_ops";
        }
        if (normalized.contains("VIEWER") || normalized.contains("DATA_VIEWER")) {
            return "observer";
        }
        return "observer";
    }

    private boolean isTenantAdmin(Long tenantId, Long userId) {
        List<String> roleCodes = authQueryMapper.findRoleCodes(tenantId, userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .anyMatch("TENANT_ADMIN"::equals);
    }

    private String roleNameZh(String roleKey) {
        return switch (roleKey) {
            case "admin" -> "\u7ba1\u7406\u5458";
            case "warehouse_ops" -> "\u4ed3\u50a8\u8fd0\u8425";
            case "cs_ops" -> "\u5ba2\u670d\u8fd0\u8425";
            default -> "\u89c2\u5bdf\u5458";
        };
    }
}
