-- v1.2 Sprint 4: Agent patrol, notifications, reports, audit cleanup

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE notifications ADD COLUMN severity VARCHAR(16) NOT NULL DEFAULT ''INFO'' AFTER content',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'notifications'
    AND column_name = 'severity'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE notifications ADD COLUMN jump_path VARCHAR(255) NULL AFTER read_at',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'notifications'
    AND column_name = 'jump_path'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_notify_biz_receiver ON notifications (tenant_id, biz_type, biz_no, receiver_user_id, notify_type)',
    'SELECT 1'
  )
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'notifications'
    AND index_name = 'idx_notify_biz_receiver'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO permissions (id, perm_code, perm_name, resource, action, status, created_at, updated_at, deleted)
SELECT *
FROM (
  SELECT 3120 AS id, 'NOTIFY_UPDATE' AS perm_code, '通知已读操作' AS perm_name, '/api/v1/notifications/{id}/read' AS resource, 'POST' AS action, 'ACTIVE' AS status, NOW(3) AS created_at, NOW(3) AS updated_at, 0 AS deleted UNION ALL
  SELECT 3121, 'REPORT_CENTER_READ', '报表中心查看', '/api/v1/reports', 'GET', 'ACTIVE', NOW(3), NOW(3), 0 UNION ALL
  SELECT 3122, 'REPORT_EXPORT', '报表导出', '/api/v1/reports/*/export', 'GET', 'ACTIVE', NOW(3), NOW(3), 0 UNION ALL
  SELECT 3123, 'AGENT_PATROL_READ', 'Agent 巡检查看', '/api/v1/agent/patrol', 'GET', 'ACTIVE', NOW(3), NOW(3), 0 UNION ALL
  SELECT 3124, 'AGENT_PATROL_RUN', 'Agent 巡检执行', '/api/v1/agent/patrol/run', 'POST', 'ACTIVE', NOW(3), NOW(3), 0 UNION ALL
  SELECT 3125, 'AUDIT_CLEANUP_RUN', '审计清理执行', '/api/v1/audit-logs/cleanup', 'POST', 'ACTIVE', NOW(3), NOW(3), 0
) p
WHERE NOT EXISTS (SELECT 1 FROM permissions existing WHERE existing.perm_code = p.perm_code);

INSERT INTO role_permissions (id, tenant_id, role_id, permission_id, created_at, updated_at, deleted)
SELECT 9000000 + r.id * 10000 + p.id, 1, r.id, p.id, NOW(3), NOW(3), 0
FROM roles r
JOIN permissions p ON p.perm_code IN ('NOTIFY_READ','NOTIFY_UPDATE','REPORT_CENTER_READ','REPORT_EXPORT','AGENT_PATROL_READ','AGENT_PATROL_RUN','AUDIT_CLEANUP_RUN')
WHERE r.role_code = 'TENANT_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0);

INSERT INTO role_permissions (id, tenant_id, role_id, permission_id, created_at, updated_at, deleted)
SELECT 9100000 + r.id * 10000 + p.id, 1, r.id, p.id, NOW(3), NOW(3), 0
FROM roles r
JOIN permissions p ON p.perm_code IN ('NOTIFY_READ','NOTIFY_UPDATE','REPORT_CENTER_READ','REPORT_EXPORT','AGENT_PATROL_READ')
WHERE r.role_code IN ('WAREHOUSE_MANAGER','WAREHOUSE_OPERATOR','CS_AGENT')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0);

INSERT INTO role_permissions (id, tenant_id, role_id, permission_id, created_at, updated_at, deleted)
SELECT 9200000 + r.id * 10000 + p.id, 1, r.id, p.id, NOW(3), NOW(3), 0
FROM roles r
JOIN permissions p ON p.perm_code IN ('NOTIFY_READ','NOTIFY_UPDATE','REPORT_CENTER_READ','AGENT_PATROL_READ')
WHERE r.role_code IN ('OBSERVER','DATA_VIEWER')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.deleted = 0);
