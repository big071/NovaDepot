package com.novadepot.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminResetPasswordRequest {
    @NotBlank
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
