# NovaDepot 商用版本 Phase 1 基础能力推进（2026-04-13）

## 1. 当前差距清单（相对商用基础版）
1. 数据层仍以单一 `99-seed-mvp.sql` 为主，缺少“演示数据 / 业务样本 / 压测样本”分层执行机制。
2. `audit_logs` 表已存在，但审计查询服务仍为 mock 返回，未形成真实审计闭环。
3. 关键业务动作（登录、入库过账、出库发运、客服建单、AI 会话）缺少统一审计落库。
4. 密码策略虽已有 BCrypt 升级机制，但缺少显式复杂度策略与配置化口径。
5. 备份恢复、发布回滚分散在多份文档中，缺少一份面向商用 Phase 1 的统一执行说明。

## 2. 本轮执行范围
1. 文档优先：输出 Phase 1 方案、数据分层方案、执行命令、回滚与验收路径。
2. 数据分层：新增业务样本种子脚本、轻量压测样本脚本、业务样本重置脚本。
3. 审计能力：
   - 将审计日志列表/详情改为 MySQL 真实查询。
   - 为关键动作增加审计落库（最小可用，不做大范围 AOP 重构）。
4. 密码策略：增加配置化密码复杂度校验工具，并在登录入口执行最小策略保护（空口令/弱口令拦截策略仅用于新策略生效用户）。
5. 保持现有接口契约与 Docker 本地运行兼容，不新增无关模块。

## 3. 数据分层策略
1. 演示数据层：`99-seed-mvp.sql`
   - 用于默认演示和日常联调。
2. 业务样本层：`100-seed-business-sample.sql`
   - 用于商用场景演示：更真实的单据、会话、FAQ、AI 历史样本。
3. 压测样本层：`101-seed-stress-lite.sql`
   - 用于列表分页、查询性能与前端加载稳定性验证（轻量规模）。
4. 重置脚本：`96-reset-business-sample.sql`
   - 清理业务样本层和轻量压测层可重灌数据，保留核心租户与基础权限。

## 4. 本轮计划修改文件
1. `docs/41-phase1-commercial-foundation-2026-04-13.md`
2. `backend/deploy/mysql/init/96-reset-business-sample.sql`
3. `backend/deploy/mysql/init/100-seed-business-sample.sql`
4. `backend/deploy/mysql/init/101-seed-stress-lite.sql`
5. `backend/src/main/java/com/novadepot/backend/repository/AuditLogMapper.java`
6. `backend/src/main/java/com/novadepot/backend/modules/auditlogs/AuditLogsService.java`
7. `backend/src/main/java/com/novadepot/backend/modules/auditlogs/AuditLogRecordService.java`
8. `backend/src/main/java/com/novadepot/backend/modules/auth/AuthService.java`
9. `backend/src/main/resources/application.yml`
10. `backend/src/main/resources/application-dev.yml`
11. `backend/src/main/resources/application-docker.yml`
12. `backend/src/main/java/com/novadepot/backend/modules/inboundorders/InboundOrdersService.java`
13. `backend/src/main/java/com/novadepot/backend/modules/outboundorders/OutboundOrdersService.java`
14. `backend/src/main/java/com/novadepot/backend/modules/customerservice/CustomerServiceService.java`
15. `backend/src/main/java/com/novadepot/backend/modules/ai/AiService.java`

## 5. 验收标准
1. 审计日志列表与详情返回真实数据库记录。
2. 登录、入库、出库、客服建单、AI 会话至少覆盖 1 条审计日志写入。
3. 数据分层脚本可重复执行，且执行后关键页面默认非空。
4. 提供 Docker 下可执行的重置与重灌命令。
5. 保持当前主链路可运行，不引入无关重构。

## 6. 执行命令（Docker）
1. 重置业务样本层
   - `Get-Content backend/deploy/mysql/init/96-reset-business-sample.sql | docker compose exec -T mysql mysql -uroot -proot novadepot`
2. 灌入演示基础 + 业务样本 + 压测样本
   - `Get-Content backend/deploy/mysql/init/99-seed-mvp.sql | docker compose exec -T mysql mysql -uroot -proot novadepot`
   - `Get-Content backend/deploy/mysql/init/100-seed-business-sample.sql | docker compose exec -T mysql mysql -uroot -proot novadepot`
   - `Get-Content backend/deploy/mysql/init/101-seed-stress-lite.sql | docker compose exec -T mysql mysql -uroot -proot novadepot`

## 7. 回滚说明（本轮）
1. 代码回滚：按文件粒度回滚本轮改动，优先回滚审计插桩。
2. 数据回滚：执行 `97-reset-demo-data.sql` + `99-seed-mvp.sql` 恢复到 RC 演示基线。
3. 风险控制：若审计插桩影响主链路，可先保留审计查询改造，临时关闭插桩调用。

## 8. 完成记录（本轮结束后补充）
### 8.1 已完成能力（本轮）
1. 数据分层能力落地：
   - 新增 `96-reset-business-sample.sql`（业务样本/压测样本重置）
   - 新增 `100-seed-business-sample.sql`（真实业务样本）
   - 新增 `101-seed-stress-lite.sql`（轻量压测样本）
2. 审计日志从 mock 升级为 MySQL 查询：
   - `/api/v1/audit-logs` 支持分页与筛选（`pageNo/pageSize/module/action/resourceType`）
   - `/api/v1/audit-logs/{id}` 返回真实详情
3. 关键业务动作审计落库（最小可用）：
   - 登录成功/失败
   - 入库：创建/审核/过账
   - 出库：创建/审核/发运
   - 客服：建工单
   - AI：发消息（会话写入）
4. 登录密码策略增强（配置化）：
   - 新增 `app.auth.password.min-length`
   - 新增 `app.auth.password.enforce-strength-on-login`
   - 默认 `false`（保持现网兼容，可逐步打开）

### 8.2 验证结果（2026-04-13）
1. SQL 验证：
   - `96-reset-business-sample.sql` 执行通过
   - `100-seed-business-sample.sql` 执行通过
   - `101-seed-stress-lite.sql` 执行通过
2. 数据量抽样：
   - `products`（ID 300000-399999）：`66`
   - `inventory`（ID 300000-399999）：`66`
   - `inbound_orders`：`4`
   - `outbound_orders`：`4`
   - `customer_service_sessions`：`3`
   - `ai_conversations`：`2`
   - `audit_logs`：`4`（初始样本，不含运行时新增）
3. API 验证：
   - 登录成功：`POST /api/v1/auth/login`
   - 审计分页成功：`GET /api/v1/audit-logs?pageNo=1&pageSize=5`
   - 入库创建->审核->过账后，审计存在 `CREATE/APPROVE/POST`
   - 出库创建->审核->发运后，审计存在 `CREATE/APPROVE/SHIP`
   - 客服建单和 AI 发消息后，审计存在 `CS/CREATE_TICKET`、`AI/CHAT`

### 8.3 本轮未完成差距
1. 审计目前仅覆盖关键链路，尚未覆盖全部写操作（如商品/仓库主数据变更、设置变更等）。
2. 密码策略已配置化，但尚未补齐“改密/重置密码”全流程策略执行与历史密码防复用。
3. 导入导出能力尚未落代码（本轮未扩展新接口，避免超范围改动）。
4. 备份恢复与发布回滚已有执行口径，但尚未加入自动化脚本与 CI 验证任务。

### 8.4 备份恢复与回滚说明（Phase 1 口径）
1. 备份（Docker 本地）：
   - `docker compose exec -T mysql mysqldump -uroot -proot --single-transaction novadepot > backup-novadepot.sql`
2. 恢复：
   - `Get-Content backup-novadepot.sql | docker compose exec -T mysql mysql -uroot -proot novadepot`
3. 数据重置到 RC 演示基线：
   - `97-reset-demo-data.sql` + `99-seed-mvp.sql`
4. 数据重置到商用样本基线：
   - `96-reset-business-sample.sql` + `99-seed-mvp.sql` + `100-seed-business-sample.sql` + `101-seed-stress-lite.sql`

### 8.5 下一阶段建议（优先级）
1. P1：补“用户改密/管理员重置密码”接口与审计闭环，正式启用强密码策略。
2. P1：补导入导出最小能力（商品/库存 CSV 导入导出）并接入权限与审计。
3. P2：扩展审计覆盖到主数据变更与系统设置变更。
4. P2：将备份恢复命令脚本化并纳入 CI 夜间任务。

## 9. 本轮增量计划（Phase 1-Import/Export）
1. 目标：在不扩展模块边界的前提下，为商用基础版补齐“商品/库存导入导出最小能力”。  
2. 范围：
   - 商品：CSV 导出、CSV 导入（最小字段：`productCode,productName,categoryId,unitId,barcode`）
   - 库存：CSV 导出（核心字段）
   - RBAC：新增权限点并授予角色
   - 审计：导入导出动作入库
3. 非目标：
   - 不做 Excel 模板管理
   - 不做异步大文件导入
   - 不做历史导入任务中心
4. Docker 执行说明：
   - 依赖现有 API 进程与 MySQL，不新增外部组件
   - 权限点通过 `99-seed-mvp.sql` 可重复初始化

## 10. 本轮增量完成记录（Phase 1-Import/Export）
1. 商品导入导出：
   - 新增 `GET /api/v1/products/export`（CSV 导出）
   - 新增 `POST /api/v1/products/import`（CSV 导入，最小字段）
2. 库存导出：
   - 新增 `GET /api/v1/inventory/export`（CSV 导出）
3. RBAC 权限点：
   - 新增 `PRODUCT_EXPORT`、`PRODUCT_IMPORT`、`INVENTORY_EXPORT`
   - 已在 `99-seed-mvp.sql` 写入并授予 `TENANT_ADMIN`、`WAREHOUSE_MANAGER`（导入+导出）
4. 审计日志：
   - 商品导入/导出、库存导出动作写入 `audit_logs`
5. 兼容性：
   - 未变更既有主链路接口契约
   - Docker 本地启动方式不变
