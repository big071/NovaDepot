SET NAMES utf8mb4;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE ai_conversations ADD COLUMN last_active_at DATETIME(3) NULL AFTER ended_at',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_conversations'
    AND column_name = 'last_active_at'
);
PREPARE s_v12_s2_conv_last_active FROM @stmt;
EXECUTE s_v12_s2_conv_last_active;
DEALLOCATE PREPARE s_v12_s2_conv_last_active;

UPDATE ai_conversations
SET last_active_at = COALESCE(updated_at, started_at, NOW(3))
WHERE last_active_at IS NULL;

UPDATE ai_conversations
SET status = CASE
  WHEN status = 'OPEN' THEN 'ACTIVE'
  WHEN status = 'CLOSED' THEN 'ARCHIVED'
  ELSE status
END
WHERE status IN ('OPEN', 'CLOSED');

UPDATE ai_conversations
SET ended_at = COALESCE(ended_at, updated_at, last_active_at, NOW(3))
WHERE status = 'ARCHIVED'
  AND ended_at IS NULL;

SET @stmt = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE ai_conversations MODIFY COLUMN last_active_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_conversations'
    AND column_name = 'last_active_at'
);
PREPARE s_v12_s2_conv_last_active_not_null FROM @stmt;
EXECUTE s_v12_s2_conv_last_active_not_null;
DEALLOCATE PREPARE s_v12_s2_conv_last_active_not_null;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE ai_messages ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''COMPLETED'' AFTER error_code',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_messages'
    AND column_name = 'status'
);
PREPARE s_v12_s2_msg_status FROM @stmt;
EXECUTE s_v12_s2_msg_status;
DEALLOCATE PREPARE s_v12_s2_msg_status;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_ai_conv_status_active ON ai_conversations (tenant_id, status, last_active_at)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_conversations'
    AND index_name = 'idx_ai_conv_status_active'
);
PREPARE s_v12_s2_conv_status_idx FROM @stmt;
EXECUTE s_v12_s2_conv_status_idx;
DEALLOCATE PREPARE s_v12_s2_conv_status_idx;

SET @stmt = (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_ai_msg_conv_status ON ai_messages (tenant_id, conversation_id, status, created_at)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_messages'
    AND index_name = 'idx_ai_msg_conv_status'
);
PREPARE s_v12_s2_msg_status_idx FROM @stmt;
EXECUTE s_v12_s2_msg_status_idx;
DEALLOCATE PREPARE s_v12_s2_msg_status_idx;
