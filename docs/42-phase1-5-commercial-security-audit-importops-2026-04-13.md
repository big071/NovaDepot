# NovaDepot 商用版本 Phase 1.5：账号安全、审计扩展、导入导出增强、运维脚本化（2026-04-13）

## 1. 目标与边界

### 1.1 本轮目标
在 Phase 1 基础上补齐商用最关键能力：
1. 账号与安全闭环
2. 审计扩展与可检索性增强
3. 导入导出增强
4. 运维脚本化

### 1.2 非目标
1. 不进入 Agent 开发
2. 不扩展多租户
3. 不扩展计费体系
4. 不做大规模无关重构

## 2. 当前差距清单
1. 缺少用户改密、管理员重置密码、首次登录强制改密完整流程。
2. 缺少登录失败次数限制与短时锁定。
3. 强密码策略仅有登录拦截开关，缺少正式配置与改密/重置统一校验。
4. 缺少改密/重置密码审计落库。
5. 审计覆盖不完整（商品编辑、仓库编辑、库位编辑、FAQ 编辑、工单状态更新缺失）。
6. 审计筛选维度偏少，详情展示 before/after 可读性不足。
7. 商品导入缺少模板下载与错误报告；库存尚无 CSV 导入。
8. 运维尚缺一键备份/恢复/重置与发布回滚检查清单脚本化落地。

## 3. 本轮执行范围

### 3.1 账号与安全
1. 新增用户修改密码接口。
2. 新增管理员重置密码接口。
3. 新增首次登录强制改密（最小可用）：通过用户标记位触发。
4. 强密码策略正式启用方案：
   1. 配置化最小长度
   2. 是否要求大小写/数字/特殊字符
   3. 是否禁止与用户名相同
5. 登录失败次数限制与短时锁定：
   1. 达阈值后记录锁定截止时间
   2. 锁定期内拒绝登录
6. 改密/重置密码审计落库。

### 3.2 审计扩展
1. 覆盖动作扩展到：
   1. 商品新增/编辑/导入/导出
   2. 仓库新增/编辑
   3. 库位新增/编辑
   4. FAQ 编辑
   5. 工单状态更新
2. 审计筛选增强：支持 `module/action/resourceType/resourceId/bizNo/operatorId/dateFrom/dateTo`。
3. 审计详情最小可用增强：返回 `beforeJson/afterJson` 的解析对象，并提供扁平 diff 摘要。

### 3.3 导入导出增强
1. 库存 CSV 导入。
2. 商品导入模板接口。
3. 商品导入错误报告（CSV 文本）。
4. 导入结果摘要（总行数、成功、失败、错误数）。
5. 导出字段说明接口。
6. 全流程对齐 RBAC 与审计。

### 3.4 运维脚本化
1. 一键备份脚本。
2. 一键恢复脚本。
3. 一键重置到 RC 基线脚本。
4. 一键重置到商用样本基线脚本。
5. Docker 下执行说明。
6. 发布与回滚检查清单脚本化方案。

## 4. 设计口径（最小可用）

### 4.1 账号安全数据字段
在 `users` 表新增：
1. `force_password_change`：首次登录或管理员重置后为 `1`。
2. `failed_login_count`：连续失败次数。
3. `lock_until`：短时锁定截止时间。
4. `pwd_updated_at`：密码更新时间。

### 4.2 登录流程
1. 用户不存在：计审计失败。
2. 用户被锁定（`lock_until > now`）：拒绝登录，返回剩余锁定秒数。
3. 密码错误：失败次数 +1；达到阈值则设置 `lock_until` 并归零计数。
4. 密码正确：清空失败计数与锁定时间。
5. 若 `force_password_change=1`：返回 `mustChangePassword=true`（先最小可用，不阻止拿 token）。

### 4.3 密码策略
统一在以下流程校验：
1. 用户改密
2. 管理员重置密码

策略项来自配置：
1. 最小长度
2. 大写字母要求
3. 小写字母要求
4. 数字要求
5. 特殊字符要求
6. 禁止包含用户名

### 4.4 审计标准
1. 写操作必须记录 `module/action/resourceType/resourceId/bizNo`。
2. `beforeJson/afterJson` 尽量填充关键字段。
3. 敏感字段（如密码哈希）不落审计正文。

### 4.5 导入导出格式
1. 商品导入模板字段：`productCode,productName,categoryId,unitId,barcode`。
2. 库存导入字段：`warehouseId,locationId,productId,availableQty,lockedQty,inTransitQty`。
3. 错误报告字段：`lineNo,error,rawLine`。

## 5. 权限点规划
新增权限点（并在 `TENANT_ADMIN`、`WAREHOUSE_MANAGER`、`CS_AGENT` 合理授权）：
1. `USER_CHANGE_PASSWORD`
2. `USER_RESET_PASSWORD`
3. `PRODUCT_UPDATE`
4. `WAREHOUSE_UPDATE`
5. `LOCATION_UPDATE`
6. `CS_FAQ_UPDATE`
7. `CS_TICKET_UPDATE`
8. `INVENTORY_IMPORT`
9. `PRODUCT_TEMPLATE_EXPORT`
10. `IMPORT_ERROR_REPORT_READ`

## 6. 本轮修改文件清单（计划）
1. `docs/42-phase1-5-commercial-security-audit-importops-2026-04-13.md`
2. `backend/deploy/mysql/init/01-schema-iam-master.sql`
3. `backend/deploy/mysql/init/99-seed-mvp.sql`
4. `backend/src/main/resources/application.yml`
5. `backend/src/main/resources/application-dev.yml`
6. `backend/src/main/resources/application-docker.yml`
7. `backend/src/main/java/com/novadepot/backend/modules/auth/AuthUserRow.java`
8. `backend/src/main/java/com/novadepot/backend/repository/AuthQueryMapper.java`
9. `backend/src/main/java/com/novadepot/backend/modules/auth/AuthController.java`
10. `backend/src/main/java/com/novadepot/backend/modules/auth/AuthService.java`
11. `backend/src/main/java/com/novadepot/backend/modules/auditlogs/AuditLogsController.java`
12. `backend/src/main/java/com/novadepot/backend/modules/auditlogs/AuditLogsService.java`
13. `backend/src/main/java/com/novadepot/backend/modules/auditlogs/AuditLogRecordService.java`
14. `backend/src/main/java/com/novadepot/backend/modules/products/ProductsController.java`
15. `backend/src/main/java/com/novadepot/backend/modules/products/ProductsService.java`
16. `backend/src/main/java/com/novadepot/backend/modules/warehouses/WarehousesController.java`
17. `backend/src/main/java/com/novadepot/backend/modules/warehouses/WarehousesService.java`
18. `backend/src/main/java/com/novadepot/backend/modules/locations/LocationsController.java`
19. `backend/src/main/java/com/novadepot/backend/modules/locations/LocationsService.java`
20. `backend/src/main/java/com/novadepot/backend/modules/inventory/InventoryController.java`
21. `backend/src/main/java/com/novadepot/backend/modules/inventory/InventoryService.java`
22. `backend/src/main/java/com/novadepot/backend/modules/customerservice/CustomerServiceController.java`
23. `backend/src/main/java/com/novadepot/backend/modules/customerservice/CustomerServiceService.java`
24. `frontend-vue/src/services/auth.ts`
25. `frontend-vue/src/stores/auth.ts`
26. `frontend-vue/src/pages/LoginPage.vue`
27. `frontend-vue/src/services/wms.ts`
28. `frontend-vue/src/pages/wms/ProductsPage.vue`
29. `frontend-vue/src/pages/wms/InventoryPage.vue`
30. `frontend-vue/src/services/customerService.ts`
31. `frontend-vue/src/pages/cs/CustomerServicePage.vue`
32. `scripts/ops/backup.ps1`
33. `scripts/ops/restore.ps1`
34. `scripts/ops/reset-rc-baseline.ps1`
35. `scripts/ops/reset-commercial-baseline.ps1`
36. `scripts/ops/release-checklist.ps1`
37. `scripts/ops/rollback-checklist.ps1`
38. `scripts/ops/README.md`

## 7. 验收标准
1. 用户改密、管理员重置密码、首次登录强制改密可跑通。
2. 登录失败锁定策略生效并可恢复。
3. 改密/重置密码有审计日志。
4. 审计扩展动作全部有记录，且筛选与详情可用。
5. 库存 CSV 导入可用，商品模板/错误报告/摘要可用。
6. 导入导出权限和审计一致。
7. 一键备份/恢复/重置/检查清单脚本可在 Docker 本地执行。

## 8. Docker 执行说明（本轮目标）
1. 备份：`./scripts/ops/backup.ps1`
2. 恢复：`./scripts/ops/restore.ps1 -BackupFile <path>`
3. 重置 RC 基线：`./scripts/ops/reset-rc-baseline.ps1`
4. 重置商用样本基线：`./scripts/ops/reset-commercial-baseline.ps1`
5. 发布检查：`./scripts/ops/release-checklist.ps1`
6. 回滚检查：`./scripts/ops/rollback-checklist.ps1`

## 9. 完成记录（待本轮结束后补充）
### 9.1 已完成能力（本轮）
1. 账号与安全：
   1. 新增 `POST /api/v1/auth/change-password`（用户改密）。
   2. 新增 `POST /api/v1/auth/users/{id}/reset-password`（管理员重置密码）。
   3. 登录返回 `mustChangePassword`，支持首次登录强制改密最小闭环。
   4. 登录失败计数与短时锁定落地（阈值/锁定时长可配置）。
   5. 改密/重置密码动作写入审计。
2. 审计扩展：
   1. 商品新增/编辑/导入/导出全覆盖。
   2. 仓库新增/编辑、库位新增/编辑覆盖。
   3. FAQ 编辑、工单状态更新覆盖。
   4. 审计列表筛选扩展到 `resourceId/bizNo/operatorId/dateFrom/dateTo`。
   5. 审计详情支持 `beforeObject/afterObject/diff` 最小可用展示。
3. 导入导出增强：
   1. 新增库存 CSV 导入 `POST /api/v1/inventory/import`。
   2. 新增商品导入模板 `GET /api/v1/products/import/template`。
   3. 新增商品导入错误报告 `GET /api/v1/products/import/errors/{reportId}`。
   4. 导入结果摘要统一返回（总行、成功、失败、错误数）。
   5. 新增库存导出字段说明 `GET /api/v1/inventory/export/fields`。
4. 运维脚本化：
   1. `scripts/ops/backup.ps1`
   2. `scripts/ops/restore.ps1`
   3. `scripts/ops/reset-rc-baseline.ps1`
   4. `scripts/ops/reset-commercial-baseline.ps1`
   5. `scripts/ops/release-checklist.ps1`
   6. `scripts/ops/rollback-checklist.ps1`
   7. `scripts/ops/README.md`

### 9.2 验证记录（本轮）
1. 后端编译：`docker compose build backend` 成功。
2. 前端类型检查：`npm run typecheck` 成功。
3. 本轮新增脚本文件已落盘，路径可见且可执行。
