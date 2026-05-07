package com.novadepot.backend.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String provider = "deepseek-chat";
    private String model = "deepseek-chat";
    private boolean paidEnabled = false;
    private String systemPrompt = "你是 NovaDepot 智能仓库管理系统的 AI 助手。你的职责包括帮助用户管理库存、采购、销售、入库、出库、工单、产品信息和往来单位。请用专业、简洁的中文回答问题。如果你不确定答案，请诚实告知并建议用户查阅相关资料。";
    private Duration connectTimeout = Duration.ofMillis(5000);
    private Duration readTimeout = Duration.ofMillis(30000);
    private int maxInputChars = 4096;

    private final Paid paid = new Paid();
    private final Rule rule = new Rule();
    private final Deepseek deepseek = new Deepseek();

    public static class Paid {
        private String baseUrl;
        private String apiKey;
        private String model;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Rule {
        private int lowStockThreshold = 10;
        private int abnormalChangeThreshold = 100;

        public int getLowStockThreshold() { return lowStockThreshold; }
        public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
        public int getAbnormalChangeThreshold() { return abnormalChangeThreshold; }
        public void setAbnormalChangeThreshold(int abnormalChangeThreshold) { this.abnormalChangeThreshold = abnormalChangeThreshold; }
    }

    public static class Deepseek {
        private boolean enabled = true;
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey;
        private String chatModel = "deepseek-chat";
        private String reasonerModel = "deepseek-reasoner";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
        public String getReasonerModel() { return reasonerModel; }
        public void setReasonerModel(String reasonerModel) { this.reasonerModel = reasonerModel; }
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public boolean isPaidEnabled() { return paidEnabled; }
    public void setPaidEnabled(boolean paidEnabled) { this.paidEnabled = paidEnabled; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public int getMaxInputChars() { return maxInputChars; }
    public void setMaxInputChars(int maxInputChars) { this.maxInputChars = maxInputChars; }
    public Paid getPaid() { return paid; }
    public Rule getRule() { return rule; }
    public Deepseek getDeepseek() { return deepseek; }

    public boolean isDeepseekEnabled() { return deepseek.isEnabled(); }
    public String getDeepseekBaseUrl() { return deepseek.getBaseUrl(); }
    public String getDeepseekApiKey() { return deepseek.getApiKey(); }
    public String getDeepseekChatModel() { return deepseek.getChatModel(); }
    public String getDeepseekReasonerModel() { return deepseek.getReasonerModel(); }
}
