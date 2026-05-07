-- =========================
-- NovaDepot v1.2 Sprint 1 - AI Permissions Seed
-- =========================
SET NAMES utf8mb4;

-- New Sprint 1 AI admin permissions.
INSERT INTO permissions (id, perm_code, perm_name, resource, action, status, created_at, updated_at, deleted) VALUES
(3101,'AI_CONFIG_VIEW','AI配置查看','/api/v1/ai/config','GET','ACTIVE',NOW(3),NOW(3),0),
(3102,'AI_CONFIG_UPDATE','AI配置更新','/api/v1/ai/config','PUT','ACTIVE',NOW(3),NOW(3),0),
(3103,'AI_USAGE_LOG_VIEW','AI用量日志查看','/api/v1/ai/usage-logs','GET','ACTIVE',NOW(3),NOW(3),0)
ON DUPLICATE KEY UPDATE perm_name=VALUES(perm_name), resource=VALUES(resource), action=VALUES(action), status=VALUES(status), deleted=0, updated_at=NOW(3);

-- Grant Sprint 1 AI admin permissions to TENANT_ADMIN (role 1001).
INSERT INTO role_permissions (id, tenant_id, role_id, permission_id, created_at, updated_at, deleted)
SELECT 400000 + p.id, 1, 1001, p.id, NOW(3), NOW(3), 0
FROM permissions p
WHERE p.perm_code IN ('AI_CONFIG_VIEW','AI_CONFIG_UPDATE','AI_USAGE_LOG_VIEW')
ON DUPLICATE KEY UPDATE deleted=0, updated_at=NOW(3);
