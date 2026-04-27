package com.novadepot.backend.modules.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        Boolean mustChangePassword
) {
}
