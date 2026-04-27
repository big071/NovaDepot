package com.novadepot.backend.modules.auth;

public class AuthUserRow {
    private Long userId;
    private Long tenantId;
    private String username;
    private String realName;
    private String passwordHash;
    private String userStatus;
    private String tenantStatus;
    private Integer forcePasswordChange;
    private Integer failedLoginCount;
    private java.time.LocalDateTime lockUntil;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public String getTenantStatus() {
        return tenantStatus;
    }

    public void setTenantStatus(String tenantStatus) {
        this.tenantStatus = tenantStatus;
    }

    public Integer getForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(Integer forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }

    public Integer getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(Integer failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public java.time.LocalDateTime getLockUntil() {
        return lockUntil;
    }

    public void setLockUntil(java.time.LocalDateTime lockUntil) {
        this.lockUntil = lockUntil;
    }
}
