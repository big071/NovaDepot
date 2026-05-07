-- v1.2 Sprint 1: AI usage log table
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    conversation_id BIGINT,
    provider VARCHAR(32) NOT NULL COMMENT 'deepseek-chat/deepseek-reasoner/rule/mock/paid',
    model VARCHAR(64) COMMENT 'deepseek-chat/deepseek-reasoner',
    scene VARCHAR(32) COMMENT 'warehouse/sop/enterprise',
    role VARCHAR(16) COMMENT 'system/user/assistant',
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    latency_ms INT DEFAULT 0,
    success TINYINT(1) NOT NULL DEFAULT 1,
    error_code VARCHAR(32),
    error_message VARCHAR(512),
    cost_estimate DECIMAL(10,6) DEFAULT 0 COMMENT 'estimated cost in CNY',
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_tenant_created (tenant_id, created_at),
    INDEX idx_provider (provider),
    INDEX idx_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI usage log';
