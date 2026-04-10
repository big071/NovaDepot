# NovaDepot 数据库初始化与实体落地说明（第一版）

## 1. 目标
将 `docs/03-database-design.md` 的设计落地为可执行初始化脚本与 MyBatis-Plus 实体骨架，兼容：
- MySQL 8
- Spring Boot 3 + MyBatis-Plus
- Docker Compose 首次启动自动初始化

## 2. 初始化脚本目录（已采用）
```text
backend/deploy/mysql/init/
  00-bootstrap.sql
  01-schema-iam-master.sql
  02-schema-wms.sql
  03-schema-erp-system-ai-cs.sql
  99-seed-mvp.sql
```

说明：
- `00` 仅做引导标记
- `01~03` 做结构初始化
- `99` 做 MVP 种子数据（租户、管理员角色等）

## 3. 通用字段策略
- 所有业务表统一：
  - `id BIGINT`（雪花/分布式 ID）
  - `tenant_id BIGINT`
  - `created_at DATETIME(3)`
  - `created_by BIGINT`
  - `updated_at DATETIME(3)`
  - `updated_by BIGINT`
  - `deleted TINYINT(1)`
- 所有编码/单号字段采用租户内唯一索引。

## 4. 状态与枚举落地策略
- 以字符串状态字段为主（`VARCHAR(32)`），可读性高，便于排查与联调。
- Java 侧提供枚举常量类（`OrderStatus`、`InventoryBizType`、`AiProviderType`、`CustomerServiceStatus`）。

## 5. MyBatis-Plus 实体策略
- 统一继承 `BaseEntity`（包含审计与租户字段）。
- `@TableName` 与 SQL 表名一一映射。
- 复杂 JSON 字段先用 `String` 承载（后续可切 Jackson TypeHandler）。
- MVP 阶段先保障字段映射完整，再逐步补关联对象与 VO。

## 6. 迁移策略（本地桌面友好）
- 初始化阶段：`docker-entrypoint-initdb.d` 自动执行 SQL。
- 增量阶段：引入 Flyway（建议）按版本脚本执行。
- 约定命名：`VYYYYMMDD_NNN__description.sql`
