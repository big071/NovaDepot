package com.novadepot.backend.modules.users;

import jakarta.validation.constraints.NotBlank;

public class UserCreateRequest {
    @NotBlank(message = "username 不能为空")
    private String username;
    @NotBlank(message = "realName 不能为空")
    private String realName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
