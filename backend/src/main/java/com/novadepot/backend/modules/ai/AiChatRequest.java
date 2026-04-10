package com.novadepot.backend.modules.ai;

import jakarta.validation.constraints.NotBlank;

public class AiChatRequest {
    private Long conversationId;
    private String scene;
    @NotBlank(message = "message不能为空")
    private String message;
    private String providerHint;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProviderHint() {
        return providerHint;
    }

    public void setProviderHint(String providerHint) {
        this.providerHint = providerHint;
    }
}
