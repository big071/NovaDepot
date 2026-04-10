package com.novadepot.backend.common.enums;

public enum ErrorCode {
    VALIDATION_ERROR("SYS-0001", "参数校验失败"),
    UNAUTHORIZED("AUTH-0001", "未登录或登录已过期"),
    FORBIDDEN("AUTH-0002", "无权限访问"),
    BIZ_ERROR("SYS-0002", "业务处理失败"),
    INTERNAL_ERROR("SYS-9999", "系统繁忙，请稍后重试");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
