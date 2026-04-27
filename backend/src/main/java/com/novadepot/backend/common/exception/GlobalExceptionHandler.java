package com.novadepot.backend.common.exception;

import com.novadepot.backend.common.api.ApiResponse;
import com.novadepot.backend.common.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex) {
        HttpStatus status = resolveStatus(ex.getCode());
        log.warn("biz exception code={}, message={}, traceId={}", ex.getCode(), ex.getMessage(), traceId());
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getCode(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR.code(), message, traceId());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR.code(), ex.getMessage(), traceId());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleForbidden(AccessDeniedException ex) {
        return ApiResponse.error(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message(), traceId());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("unexpected exception, traceId={}", traceId(), ex);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message(), traceId());
    }

    private String traceId() {
        return MDC.get("traceId");
    }

    private HttpStatus resolveStatus(String code) {
        if (code == null || code.isBlank()) {
            return HttpStatus.BAD_REQUEST;
        }
        // Keep auth-class errors on auth HTTP status even if code constants drift.
        if (code.startsWith("AUTH-")) {
            if (ErrorCode.UNAUTHORIZED.code().equals(code)) {
                return HttpStatus.UNAUTHORIZED;
            }
            return HttpStatus.FORBIDDEN;
        }
        if (ErrorCode.FORBIDDEN.code().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ErrorCode.UNAUTHORIZED.code().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ErrorCode.VALIDATION_ERROR.code().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
