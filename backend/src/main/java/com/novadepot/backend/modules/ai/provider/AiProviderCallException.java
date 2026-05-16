package com.novadepot.backend.modules.ai.provider;

public class AiProviderCallException extends RuntimeException {
    private final String provider;
    private final String model;
    private final String errorCode;
    private final Integer statusCode;
    private final String safeMessage;

    public AiProviderCallException(String provider,
                                   String model,
                                   String errorCode,
                                   Integer statusCode,
                                   String safeMessage,
                                   Throwable cause) {
        super(safeMessage, cause);
        this.provider = provider;
        this.model = model;
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.safeMessage = safeMessage;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getSafeMessage() {
        return safeMessage;
    }
}
