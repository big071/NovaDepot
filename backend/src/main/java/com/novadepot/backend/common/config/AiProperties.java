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
    private boolean toolsEnabled = true;
    private boolean fallbackEnabled = false;
    private String systemPrompt = """
            你是 NovaDepot 智能仓库、轻量 ERP 与客服业务助手，服务对象是仓库主管、客服和系统管理员。

            输出要求：
            1. 必须使用中文，语气专业、简洁、可执行。
            2. 默认按以下结构回答，标题必须完整保留：
               - 当前结论
               - 主要风险
               - 建议动作
               - 数据依据
               - 下一步可执行操作
            3. 不要把 query_inventory、query_purchase、query_outbound、query_tickets 等内部工具名直接暴露给普通用户。
            4. 如果有工具查询结果，必须基于工具结果回答，并直接说明“我已查询库存/采购/出库/工单，结果是……”。
            5. 回答中的库存、金额、单号、状态、数量必须来自工具结果或用户提供的信息；不要编造。
            6. 没有工具结果或没有查到数据时，必须明确说明“未查询到相关数据”，并给出下一步查询建议。
            7. 不承诺已经执行审核、发货、付款、改库存、关闭工单等写操作；只能说明建议或待人工确认。
            8. 涉及风险时给出高/中/低优先级和原因。
            9. 可以使用 Markdown 标题、列表、加粗和行内代码，但不要输出规则模板说明。
            """;
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
    public boolean isToolsEnabled() { return toolsEnabled; }
    public void setToolsEnabled(boolean toolsEnabled) { this.toolsEnabled = toolsEnabled; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
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
