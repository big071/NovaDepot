# NovaDepot 数据库设计方案（MySQL 8）

## 1. ER 关系说明（核心）

## 1.1 总体关系
- 租户与权限域：`tenants` 1:N `users`，`users` N:M `roles`，`roles` N:M `permissions`。
- 主数据域：`product_categories` 1:N `products`，`product_units` 1:N `products`，`warehouses` 1:N `warehouse_locations`。
- 库存域：`inventory` 由 `warehouse_id + location_id + product_id` 唯一定位，所有变更进入 `inventory_transactions`。
- 单据域：入库、出库、调拨、盘点、采购、销售均采用“主表 + 明细表”1:N。
- 往来域：`suppliers` 关联采购；`customers` 关联销售与客服会话。
- 智能域：`ai_conversations` 1:N `ai_messages`；`ai_prompt_templates` 被 AI 场景引用。
- 客服域：`customer_service_sessions` 1:N `customer_service_messages`，规则来自 `customer_service_rules`，知识库来自 `faq_knowledge`。
- 系统域：`notifications`、`audit_logs`、`files` 关联全业务主键与业务编号。

## 1.2 关键设计原则
- 所有业务表预留 `tenant_id`（多租户行级隔离）。
- 统一逻辑删除字段 `deleted`（`TINYINT(1)`）。
- 金额字段 `DECIMAL(18,2)`，数量字段 `DECIMAL(18,6)`。
- 时间字段优先 `DATETIME(3)`，方便审计和排序。
- MyBatis-Plus 统一主键策略：`BIGINT` 雪花/分布式 ID。

---

## 2. 核心实体清单

| 模块 | 核心实体 |
|---|---|
| 租户与权限 | tenants, users, roles, permissions, user_roles, role_permissions |
| 商品与仓库 | product_categories, product_units, products, warehouses, warehouse_locations |
| 库存与流水 | inventory, inventory_transactions |
| 入库与出库 | inbound_orders, inbound_order_items, outbound_orders, outbound_order_items |
| 调拨与盘点 | transfer_orders, transfer_order_items, stocktake_orders, stocktake_order_items |
| 采购与销售 | purchase_orders, purchase_order_items, sales_orders, sales_order_items, suppliers, customers |
| 系统协同 | notifications, audit_logs, files |
| AI | ai_conversations, ai_messages, ai_prompt_templates |
| 智能客服 | customer_service_sessions, customer_service_messages, customer_service_rules, faq_knowledge |

---

## 3. 数据库分模块设计（逐表）

说明：以下字段均建议附带审计字段（见第 6 节）。

## 3.1 租户与权限模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `tenants` | 租户信息（预留） | `id`,`tenant_code`,`tenant_name`,`status`,`expire_at` | `BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`TINYINT`,`DATETIME(3)` |
| `users` | 用户账号与基础资料 | `id`,`tenant_id`,`username`,`password_hash`,`real_name`,`phone`,`email`,`status`,`last_login_at` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(255)`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(128)`,`TINYINT`,`DATETIME(3)` |
| `roles` | 角色定义 | `id`,`tenant_id`,`role_code`,`role_name`,`data_scope`,`status` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(64)`,`VARCHAR(32)`,`TINYINT` |
| `permissions` | 资源与动作权限 | `id`,`perm_code`,`perm_name`,`resource`,`action`,`status` | `BIGINT`,`VARCHAR(128)`,`VARCHAR(64)`,`VARCHAR(128)`,`VARCHAR(32)`,`TINYINT` |
| `user_roles` | 用户-角色关联 | `id`,`tenant_id`,`user_id`,`role_id` | `BIGINT`,`BIGINT`,`BIGINT`,`BIGINT` |
| `role_permissions` | 角色-权限关联 | `id`,`tenant_id`,`role_id`,`permission_id` | `BIGINT`,`BIGINT`,`BIGINT`,`BIGINT` |

## 3.2 商品与仓库模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `product_categories` | 商品分类树 | `id`,`tenant_id`,`parent_id`,`category_code`,`category_name`,`sort_no`,`status` | `BIGINT`,`BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`INT`,`TINYINT` |
| `product_units` | 计量单位字典 | `id`,`tenant_id`,`unit_code`,`unit_name`,`precision_scale`,`status` | `BIGINT`,`BIGINT`,`VARCHAR(32)`,`VARCHAR(32)`,`INT`,`TINYINT` |
| `products` | 商品/SKU 主数据 | `id`,`tenant_id`,`product_code`,`product_name`,`category_id`,`unit_id`,`barcode`,`spec`,`batch_enabled`,`shelf_life_days`,`status` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(255)`,`BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(255)`,`TINYINT`,`INT`,`TINYINT` |
| `warehouses` | 仓库主数据 | `id`,`tenant_id`,`warehouse_code`,`warehouse_name`,`warehouse_type`,`address`,`manager_user_id`,`status` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`VARCHAR(32)`,`VARCHAR(255)`,`BIGINT`,`TINYINT` |
| `warehouse_locations` | 库位主数据 | `id`,`tenant_id`,`warehouse_id`,`location_code`,`location_name`,`location_type`,`capacity_qty`,`status` | `BIGINT`,`BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`VARCHAR(32)`,`DECIMAL(18,6)`,`TINYINT` |

## 3.3 库存与流水模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `inventory` | 库存余额快照 | `id`,`tenant_id`,`warehouse_id`,`location_id`,`product_id`,`available_qty`,`locked_qty`,`in_transit_qty`,`version_no` | `BIGINT`,`BIGINT`,`BIGINT`,`BIGINT`,`BIGINT`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`INT` |
| `inventory_transactions` | 库存变更流水（事实表） | `id`,`tenant_id`,`txn_no`,`biz_type`,`biz_no`,`warehouse_id`,`location_id`,`product_id`,`change_qty`,`before_qty`,`after_qty`,`occurred_at`,`operator_id`,`request_id` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(64)`,`BIGINT`,`BIGINT`,`BIGINT`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DATETIME(3)`,`BIGINT`,`VARCHAR(64)` |

## 3.4 入库与出库模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `inbound_orders` | 入库单主表 | `id`,`tenant_id`,`inbound_no`,`biz_type`,`status`,`warehouse_id`,`supplier_id`,`expected_at`,`completed_at`,`remark` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(32)`,`BIGINT`,`BIGINT`,`DATETIME(3)`,`DATETIME(3)`,`VARCHAR(500)` |
| `inbound_order_items` | 入库单明细 | `id`,`tenant_id`,`inbound_order_id`,`line_no`,`product_id`,`plan_qty`,`received_qty`,`qualified_qty`,`unit_id`,`batch_no`,`production_date`,`expire_date` | `BIGINT`,`BIGINT`,`BIGINT`,`INT`,`BIGINT`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`BIGINT`,`VARCHAR(64)`,`DATE`,`DATE` |
| `outbound_orders` | 出库单主表 | `id`,`tenant_id`,`outbound_no`,`biz_type`,`status`,`warehouse_id`,`customer_id`,`expected_ship_at`,`shipped_at`,`remark` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(32)`,`BIGINT`,`BIGINT`,`DATETIME(3)`,`DATETIME(3)`,`VARCHAR(500)` |
| `outbound_order_items` | 出库单明细 | `id`,`tenant_id`,`outbound_order_id`,`line_no`,`product_id`,`plan_qty`,`picked_qty`,`shipped_qty`,`unit_id`,`batch_no` | `BIGINT`,`BIGINT`,`BIGINT`,`INT`,`BIGINT`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`BIGINT`,`VARCHAR(64)` |

## 3.5 调拨与盘点模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `transfer_orders` | 调拨单主表 | `id`,`tenant_id`,`transfer_no`,`status`,`from_warehouse_id`,`to_warehouse_id`,`from_location_id`,`to_location_id`,`completed_at` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`BIGINT`,`BIGINT`,`BIGINT`,`BIGINT`,`DATETIME(3)` |
| `transfer_order_items` | 调拨单明细 | `id`,`tenant_id`,`transfer_order_id`,`line_no`,`product_id`,`plan_qty`,`actual_qty`,`unit_id`,`batch_no` | `BIGINT`,`BIGINT`,`BIGINT`,`INT`,`BIGINT`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`BIGINT`,`VARCHAR(64)` |
| `stocktake_orders` | 盘点单主表 | `id`,`tenant_id`,`stocktake_no`,`status`,`warehouse_id`,`scope_type`,`planned_at`,`started_at`,`finished_at`,`diff_count` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`BIGINT`,`VARCHAR(32)`,`DATETIME(3)`,`DATETIME(3)`,`DATETIME(3)`,`INT` |
| `stocktake_order_items` | 盘点明细 | `id`,`tenant_id`,`stocktake_order_id`,`line_no`,`product_id`,`location_id`,`system_qty`,`counted_qty`,`diff_qty`,`result_type` | `BIGINT`,`BIGINT`,`BIGINT`,`INT`,`BIGINT`,`BIGINT`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`VARCHAR(32)` |

## 3.6 采购与销售模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `suppliers` | 供应商主数据 | `id`,`tenant_id`,`supplier_code`,`supplier_name`,`contact_name`,`phone`,`credit_level`,`status` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(16)`,`TINYINT` |
| `customers` | 客户主数据 | `id`,`tenant_id`,`customer_code`,`customer_name`,`contact_name`,`phone`,`customer_level`,`status` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(16)`,`TINYINT` |
| `purchase_orders` | 采购单主表 | `id`,`tenant_id`,`purchase_no`,`status`,`supplier_id`,`warehouse_id`,`total_amount`,`expected_arrival_date` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`BIGINT`,`BIGINT`,`DECIMAL(18,2)`,`DATE` |
| `purchase_order_items` | 采购单明细 | `id`,`tenant_id`,`purchase_order_id`,`line_no`,`product_id`,`unit_price`,`order_qty`,`received_qty`,`tax_rate` | `BIGINT`,`BIGINT`,`BIGINT`,`INT`,`BIGINT`,`DECIMAL(18,2)`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DECIMAL(8,4)` |
| `sales_orders` | 销售单主表 | `id`,`tenant_id`,`sales_no`,`status`,`customer_id`,`warehouse_id`,`total_amount`,`delivery_date` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`BIGINT`,`BIGINT`,`DECIMAL(18,2)`,`DATE` |
| `sales_order_items` | 销售单明细 | `id`,`tenant_id`,`sales_order_id`,`line_no`,`product_id`,`unit_price`,`order_qty`,`shipped_qty`,`tax_rate` | `BIGINT`,`BIGINT`,`BIGINT`,`INT`,`BIGINT`,`DECIMAL(18,2)`,`DECIMAL(18,6)`,`DECIMAL(18,6)`,`DECIMAL(8,4)` |

## 3.7 系统协同模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `notifications` | 系统/业务通知 | `id`,`tenant_id`,`notify_type`,`biz_type`,`biz_no`,`receiver_user_id`,`title`,`content`,`read_flag`,`sent_at`,`read_at` | `BIGINT`,`BIGINT`,`VARCHAR(32)`,`VARCHAR(32)`,`VARCHAR(64)`,`BIGINT`,`VARCHAR(255)`,`TEXT`,`TINYINT`,`DATETIME(3)`,`DATETIME(3)` |
| `audit_logs` | 审计日志 | `id`,`tenant_id`,`module`,`action`,`resource_type`,`resource_id`,`biz_no`,`operator_id`,`operator_name`,`before_json`,`after_json`,`ip`,`user_agent`,`occurred_at` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(64)`,`VARCHAR(64)`,`VARCHAR(64)`,`VARCHAR(64)`,`BIGINT`,`VARCHAR(64)`,`JSON`,`JSON`,`VARCHAR(64)`,`VARCHAR(255)`,`DATETIME(3)` |
| `files` | 文件元数据 | `id`,`tenant_id`,`biz_type`,`biz_no`,`file_name`,`file_ext`,`mime_type`,`file_size`,`storage_provider`,`bucket`,`object_key`,`url`,`uploaded_by`,`uploaded_at` | `BIGINT`,`BIGINT`,`VARCHAR(32)`,`VARCHAR(64)`,`VARCHAR(255)`,`VARCHAR(32)`,`VARCHAR(64)`,`BIGINT`,`VARCHAR(32)`,`VARCHAR(64)`,`VARCHAR(255)`,`VARCHAR(500)`,`BIGINT`,`DATETIME(3)` |

## 3.8 AI 模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `ai_conversations` | AI 会话主表 | `id`,`tenant_id`,`conversation_no`,`scene`,`biz_type`,`biz_no`,`provider_type`,`model_name`,`status`,`started_at`,`ended_at` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(32)`,`VARCHAR(64)`,`VARCHAR(32)`,`VARCHAR(64)`,`VARCHAR(32)`,`DATETIME(3)`,`DATETIME(3)` |
| `ai_messages` | AI 会话消息 | `id`,`tenant_id`,`conversation_id`,`role`,`content`,`tokens`,`latency_ms`,`confidence`,`error_code`,`created_at` | `BIGINT`,`BIGINT`,`BIGINT`,`VARCHAR(16)`,`TEXT`,`INT`,`INT`,`DECIMAL(5,4)`,`VARCHAR(64)`,`DATETIME(3)` |
| `ai_prompt_templates` | 提示词模板 | `id`,`tenant_id`,`template_code`,`template_name`,`scene`,`template_content`,`version_no`,`enabled`,`remark` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`VARCHAR(32)`,`TEXT`,`INT`,`TINYINT`,`VARCHAR(500)` |

## 3.9 智能客服模块

| 表名 | 用途 | 关键字段 | 字段类型建议 |
|---|---|---|---|
| `customer_service_sessions` | 客服会话主表 | `id`,`tenant_id`,`session_no`,`channel`,`customer_id`,`status`,`assigned_user_id`,`priority`,`first_response_at`,`closed_at` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(32)`,`BIGINT`,`VARCHAR(32)`,`BIGINT`,`VARCHAR(16)`,`DATETIME(3)`,`DATETIME(3)` |
| `customer_service_messages` | 客服消息 | `id`,`tenant_id`,`session_id`,`sender_type`,`sender_id`,`content`,`msg_type`,`ai_suggested`,`created_at` | `BIGINT`,`BIGINT`,`BIGINT`,`VARCHAR(16)`,`BIGINT`,`TEXT`,`VARCHAR(16)`,`TINYINT`,`DATETIME(3)` |
| `customer_service_rules` | 客服规则配置 | `id`,`tenant_id`,`rule_code`,`rule_name`,`trigger_type`,`trigger_expr`,`action_type`,`action_config`,`enabled` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(128)`,`VARCHAR(32)`,`TEXT`,`VARCHAR(32)`,`JSON`,`TINYINT` |
| `faq_knowledge` | FAQ 知识库 | `id`,`tenant_id`,`faq_code`,`question`,`answer`,`tags`,`scene`,`priority`,`enabled`,`version_no` | `BIGINT`,`BIGINT`,`VARCHAR(64)`,`VARCHAR(500)`,`TEXT`,`VARCHAR(255)`,`VARCHAR(32)`,`INT`,`TINYINT`,`INT` |

---

## 4. 主键、外键、索引与唯一约束建议

## 4.1 主键与外键
- 主键：所有表 `id BIGINT` 作为单列主键。
- 外键：生产环境可优先“逻辑外键”（应用层约束）以降低高并发写入压力；初始化脚本可选择仅对核心关系加物理外键（如明细到主单）。
- 推荐物理外键（可选）：`*_items.*_order_id -> *_orders.id`。

## 4.2 索引建议（高优先）
| 表 | 索引建议 |
|---|---|
| `users` | `uk_tenant_username(tenant_id, username)`；`idx_tenant_status(tenant_id, status)` |
| `roles` | `uk_tenant_role_code(tenant_id, role_code)` |
| `permissions` | `uk_perm_code(perm_code)` |
| `products` | `uk_tenant_product_code(tenant_id, product_code)`；`idx_tenant_barcode(tenant_id, barcode)` |
| `warehouses` | `uk_tenant_wh_code(tenant_id, warehouse_code)` |
| `warehouse_locations` | `uk_tenant_wh_loc_code(tenant_id, warehouse_id, location_code)` |
| `inventory` | `uk_tenant_wh_loc_prod(tenant_id, warehouse_id, location_id, product_id)` |
| `inventory_transactions` | `idx_tenant_biz(tenant_id, biz_type, biz_no)`；`uk_tenant_request_id(tenant_id, request_id)`；`idx_tenant_time(tenant_id, occurred_at)` |
| `inbound_orders` | `uk_tenant_inbound_no(tenant_id, inbound_no)`；`idx_tenant_status_time(tenant_id, status, created_at)` |
| `outbound_orders` | `uk_tenant_outbound_no(tenant_id, outbound_no)`；`idx_tenant_status_time(tenant_id, status, created_at)` |
| `transfer_orders` | `uk_tenant_transfer_no(tenant_id, transfer_no)` |
| `stocktake_orders` | `uk_tenant_stocktake_no(tenant_id, stocktake_no)` |
| `purchase_orders` | `uk_tenant_purchase_no(tenant_id, purchase_no)` |
| `sales_orders` | `uk_tenant_sales_no(tenant_id, sales_no)` |
| `notifications` | `idx_receiver_read(receiver_user_id, read_flag)`；`idx_tenant_sent(tenant_id, sent_at)` |
| `audit_logs` | `idx_tenant_module_time(tenant_id, module, occurred_at)`；`idx_resource(resource_type, resource_id)` |
| `ai_conversations` | `uk_tenant_conv_no(tenant_id, conversation_no)`；`idx_scene_time(tenant_id, scene, started_at)` |
| `customer_service_sessions` | `uk_tenant_session_no(tenant_id, session_no)`；`idx_assigned_status(tenant_id, assigned_user_id, status)` |

## 4.3 唯一约束建议（核心）
- 编码类字段统一租户内唯一：`*_code`、`*_no`。
- 关联关系去重：`user_roles(tenant_id,user_id,role_id)` 唯一；`role_permissions(tenant_id,role_id,permission_id)` 唯一。
- 幂等约束：`inventory_transactions(tenant_id,request_id)` 唯一。

---

## 5. 审计字段、状态字段、多租户字段设计建议

## 5.1 审计字段（建议统一到所有业务表）
| 字段 | 类型 | 说明 |
|---|---|---|
| `created_at` | `DATETIME(3)` | 创建时间 |
| `created_by` | `BIGINT` | 创建人 ID |
| `updated_at` | `DATETIME(3)` | 更新时间 |
| `updated_by` | `BIGINT` | 更新人 ID |
| `deleted` | `TINYINT(1)` | 逻辑删除标记（0/1） |

MyBatis-Plus 建议：
- 开启自动填充（MetaObjectHandler）填充 `created_at/updated_at/created_by/updated_by`。
- 使用 `@TableLogic` 对接 `deleted`。

## 5.2 状态字段设计建议
- 单据状态字段统一 `status VARCHAR(32)`，避免硬编码数字语义不清。
- 建议值示例：`DRAFT`,`SUBMITTED`,`APPROVED`,`PROCESSING`,`COMPLETED`,`CANCELED`。
- 状态变更同时写入 `audit_logs` 与业务日志。

## 5.3 多租户字段建议
| 字段 | 类型 | 说明 |
|---|---|---|
| `tenant_id` | `BIGINT` | 租户隔离主字段，所有业务表强制存在 |

MyBatis-Plus 建议：
- 启用租户拦截器（TenantLineInnerInterceptor）自动拼接 `tenant_id` 条件。

---

## 6. Redis 设计建议（配套）

| 目标 | Key 设计建议 | TTL |
|---|---|---|
| 登录会话 | `auth:token:{tenantId}:{userId}:{jti}` | 30m~7d |
| 权限缓存 | `iam:perm:{tenantId}:{userId}` | 10m |
| 库存热点缓存 | `inv:summary:{tenantId}:{warehouseId}:{productId}` | 1m~5m |
| 幂等控制 | `idem:{tenantId}:{requestId}` | 10m |
| 通知未读计数 | `notify:unread:{tenantId}:{userId}` | 5m |
| AI 限流 | `ai:quota:{tenantId}:{date}` | 按天 |

---

## 7. Docker MySQL 8 初始化与迁移建议

## 7.1 目录建议（与 Docker Compose 配合）
```text
deploy/
  docker-compose.yml
  mysql/
    init/
      00-schema.sql
      01-master-data.sql
      02-seed-dev.sql
```

本轮脚手架落地路径：
```text
backend/deploy/mysql/init/
  00-bootstrap.sql
  01-schema-iam-master.sql
  02-schema-wms.sql
  03-schema-erp-system-ai-cs.sql
  99-seed-mvp.sql
```

## 7.2 初始化路径建议
- MySQL 容器挂载：`./deploy/mysql/init:/docker-entrypoint-initdb.d`
- 首次启动自动执行 init 脚本。
- 后续变更通过 migration 管理（推荐 Flyway/Liquibase）。

## 7.3 migration 约定建议
- 脚本命名：`V20260410_001__create_wms_tables.sql`
- 每次变更只做增量，不覆盖历史脚本。

---

## 8. 第一版必须建的表 vs 后续扩展表

## 8.1 第一版必须建的表（MVP）
- 权限与账号：`users`,`roles`,`permissions`,`user_roles`,`role_permissions`
- 主数据：`product_categories`,`product_units`,`products`,`warehouses`,`warehouse_locations`,`suppliers`,`customers`
- 库存核心：`inventory`,`inventory_transactions`
- 单据核心：`inbound_orders`,`inbound_order_items`,`outbound_orders`,`outbound_order_items`,`transfer_orders`,`transfer_order_items`,`stocktake_orders`,`stocktake_order_items`
- 业务协同：`purchase_orders`,`purchase_order_items`,`sales_orders`,`sales_order_items`
- 系统能力：`notifications`,`audit_logs`,`files`
- AI/客服基础：`ai_conversations`,`ai_messages`,`ai_prompt_templates`,`customer_service_sessions`,`customer_service_messages`,`customer_service_rules`,`faq_knowledge`

## 8.2 后续扩展表（建议）
- 租户商用：`tenants`（MVP 可先建空实现并启用单租户）
- 审批流：`approval_instances`,`approval_tasks`（若审批复杂度升级）
- 计费与套餐：`plans`,`subscriptions`,`billing_records`
- AI 配置扩展：`ai_provider_configs`,`ai_quota_logs`

---

## 9. SQL 参考片段（仅示例）

```sql
CREATE TABLE inventory (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  available_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  locked_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  in_transit_qty DECIMAL(18,6) NOT NULL DEFAULT 0,
  version_no INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  created_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_wh_loc_prod (tenant_id, warehouse_id, location_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

该片段仅用于统一字段风格与索引习惯，完整建表脚本在后续 `init.sql + migration` 阶段输出。

补充文档：
- `docs/09-db-init-and-entity-implementation.md`
