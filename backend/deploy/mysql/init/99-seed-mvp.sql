SET NAMES utf8mb4;

INSERT INTO tenants (id, tenant_code, tenant_name, status, expire_at, created_at, updated_at, deleted)
VALUES (1, 'default', 'NovaDepot 默认租户', 'ACTIVE', NULL, NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), status = VALUES(status), updated_at = NOW(3);

INSERT INTO roles (id, tenant_id, role_code, role_name, data_scope, status, created_at, updated_at, deleted)
VALUES
(1001, 1, 'TENANT_ADMIN', '租户管理员', 'ALL', 'ACTIVE', NOW(3), NOW(3), 0),
(1002, 1, 'WAREHOUSE_OPERATOR', '仓库操作员', 'WAREHOUSE', 'ACTIVE', NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), updated_at = NOW(3);

INSERT INTO users (id, tenant_id, username, password_hash, real_name, status, created_at, updated_at, deleted)
VALUES
(1, 1, 'admin', '$2a$10$replace_with_real_bcrypt_hash', '系统管理员', 'ACTIVE', NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), status = VALUES(status), updated_at = NOW(3);

INSERT INTO user_roles (id, tenant_id, user_id, role_id, created_at, updated_at, deleted)
VALUES (2001, 1, 1, 1001, NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE updated_at = NOW(3);

INSERT INTO permissions (id, perm_code, perm_name, resource, action, status, created_at, updated_at, deleted)
VALUES
(3001, 'USER_READ', '用户查询', '/api/v1/users', 'GET', 'ACTIVE', NOW(3), NOW(3), 0),
(3002, 'INVENTORY_READ', '库存查询', '/api/v1/inventory', 'GET', 'ACTIVE', NOW(3), NOW(3), 0),
(3003, 'AI_CHAT', 'AI聊天', '/api/v1/ai/chat', 'POST', 'ACTIVE', NOW(3), NOW(3), 0),
(3004, 'CS_SESSION_READ', '客服会话查询', '/api/v1/customer-service/sessions', 'GET', 'ACTIVE', NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), updated_at = NOW(3);

INSERT INTO role_permissions (id, tenant_id, role_id, permission_id, created_at, updated_at, deleted)
VALUES
(4001, 1, 1001, 3001, NOW(3), NOW(3), 0),
(4002, 1, 1001, 3002, NOW(3), NOW(3), 0),
(4003, 1, 1001, 3003, NOW(3), NOW(3), 0),
(4004, 1, 1001, 3004, NOW(3), NOW(3), 0)
ON DUPLICATE KEY UPDATE updated_at = NOW(3);
