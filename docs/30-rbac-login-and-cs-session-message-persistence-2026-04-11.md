# NovaDepot RBAC登录与客服会话持久化修复说明（2026-04-11）

## 1. 本轮问题清单
1. `admin` 无法执行入库审核/过账、出库审核/发运。
2. 客服 `sessions/messages` 仍为 mock，未与工单统一 MySQL 数据源。
3. 登录仍是“任意用户名同权限”假实现，不符合真实 RBAC。

## 2. 根因判断
1. 登录链路未查询 `users/user_roles/role_permissions/permissions`，token 权限为硬编码。
2. IAM 种子权限数据不完整，无法覆盖 WMS 审核/过账权限点。
3. 客服服务未接 `customer_service_sessions/customer_service_messages` 表。

## 3. 本轮执行范围
1. 将登录改为基于 MySQL 的真实 RBAC（按租户+用户名+密码校验，按角色关联权限发 token）。
2. 补齐并对齐权限种子，确保 `TENANT_ADMIN` 具备审核/过账相关权限。
3. 客服 `sessions/messages` 改为 MySQL 持久化读取/写入，与 `tickets` 保持统一。
4. 保持最小改动，不做无关重构。

## 4. 本轮修改文件
- `backend/src/main/java/com/novadepot/backend/modules/auth/*`
- `backend/src/main/java/com/novadepot/backend/repository/*`（新增 IAM/客服 Mapper）
- `backend/src/main/java/com/novadepot/backend/modules/customerservice/CustomerServiceService.java`
- `backend/deploy/mysql/init/99-seed-mvp.sql`
- `docs/30-rbac-login-and-cs-session-message-persistence-2026-04-11.md`

## 5. 验收标准
1. 使用 `admin / admin123 / default` 登录后，可执行入库审核/过账与出库审核/发运接口。
2. 客服会话列表与消息来自 MySQL，发送消息后可回查。
3. 登录对错误账号或密码返回明确失败，不再“任意用户名都成功”。
4. Docker 构建通过，数据库升级脚本可重复执行。

## 6. 本地升级提示
- 执行：
  - `docker compose exec mysql mysql -uroot -proot novadepot < /docker-entrypoint-initdb.d/99-seed-mvp.sql`
  - `docker compose exec mysql mysql -uroot -proot novadepot < /docker-entrypoint-initdb.d/03-schema-erp-system-ai-cs.sql`
- 该脚本使用幂等写法（`ON DUPLICATE KEY UPDATE` + 条件插入），可重复执行。

## 7. 本轮实施结果
1. 登录改为真实 RBAC：
   - 按 `tenantCode + username` 查询 `users` 与 `tenants`。
   - 按 `user_roles -> role_permissions -> permissions` 动态装载权限写入 JWT。
   - 错误用户名/密码返回 `AUTH-0001`。
2. `admin` 审核/过账权限恢复：
   - 通过 `99-seed-mvp.sql` 补齐权限点并授予 `TENANT_ADMIN` 全量权限。
   - 已验证入库审核/过账、出库审核/发运接口成功。
3. 客服会话/消息切到 MySQL：
   - `sessions/messages` 从 DB 查询，发送消息写入 `customer_service_messages`。
   - 与工单 `customer_service_tickets` 统一 MySQL 数据源。

## 8. 验证结果
1. `admin / admin123 / default` 登录成功，JWT 包含审核/过账权限。
2. 随机账号（如 `guest`）登录失败，返回明确错误信息。
3. 客服会话消息发送后重新查询可见，数据源标记为 `MYSQL`。
4. `docker compose up --build -d backend` 构建通过。
