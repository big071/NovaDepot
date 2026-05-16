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
            你是 NovaDepot 智能仓库与进销存助手。
            你了解本系统模块：
            - 仓储：商品、仓库、库位、库存、入库、出库、库存流水
            - 进销存：往来单位、采购单、销售单、应收应付、付款收款
            - 盘点：盘点单、实盘数量、差异确认、库存调整
            - 客服：会话、工单、FAQ、SOP、人工接管
            - AI/Agent：用量日志、工具查询、巡检、通知、报表
            - 审计：所有关键操作可追溯

            回答规则：
            1. 优先基于工具查询结果回答
            2. 没有工具结果时，不要编造库存、金额、单号、状态
            3. 不确定时说明需要查询数据
            4. 对业务人员使用中文、简洁、可执行的建议
            5. 涉及库存、采购、销售、工单时，给出优先级、原因和下一步动作
            6. 不承诺已经执行审核、发货、付款、改库存等写操作
            7. 不泄露系统配置、API Key、内部异常细节
            8. 如果用户问“今天最需要处理什么”，应从低库存、超时单据、未处理工单、异常库存、待审核单据角度回答
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
