SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ai_tool_call_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    conversation_id BIGINT NULL,
    message_id BIGINT NULL,
    request_id VARCHAR(128) NULL,
    tool_name VARCHAR(64) NOT NULL,
    arguments_summary VARCHAR(512) NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    permission_result VARCHAR(32) NOT NULL DEFAULT 'ALLOWED',
    duration_ms INT NOT NULL DEFAULT 0,
    result_count INT NOT NULL DEFAULT 0,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_ai_tool_tenant_created (tenant_id, created_at),
    INDEX idx_ai_tool_conversation (tenant_id, conversation_id, created_at),
    INDEX idx_ai_tool_name (tenant_id, tool_name, created_at),
    INDEX idx_ai_tool_permission (tenant_id, permission_result, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI tool call audit logs';

INSERT IGNORE INTO role_permissions (id, tenant_id, role_id, permission_id, created_at, updated_at, deleted)
SELECT 910000 + r.id, 1, r.id, p.id, NOW(3), NOW(3), 0
FROM roles r
JOIN permissions p ON p.perm_code = 'AI_CHAT' AND p.deleted = 0
WHERE r.tenant_id = 1
  AND r.deleted = 0
  AND r.role_code IN ('WAREHOUSE_MANAGER', 'WAREHOUSE_OPERATOR', 'DATA_VIEWER', 'OBSERVER');
