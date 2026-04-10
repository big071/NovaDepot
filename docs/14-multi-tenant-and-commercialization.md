# NovaDepot 多租户与商业化方案（SaaS 商用视角）

## 0. 设计前提与目标

- 当前阶段：保持单租户可运行，默认 `tenant_id=1`。
- 演进目标：支持多个商家/企业同时使用，做到数据隔离、权限隔离、套餐收费、AI 增值和租户级配置。
- 技术路线：Java 17+ + Spring Boot 3 + MySQL 8 + Redis + Docker + Docker Compose。

---

## 1. 多租户架构建议

### 1.1 推荐演进路径

| 阶段 | 架构模式 | 适用时机 | NovaDepot 建议 |
|---|---|---|---|
| S0 | 单租户（当前） | MVP/早期内测 | 保持当前模式，所有表已预留 `tenant_id` |
| S1 | 共享库共享表 + 行级隔离 | 多租户初期（10~200租户） | 首选，成本最低、上线最快 |
| S2 | 共享实例分库/分Schema | 中期（大客户增多） | 给大客户独立 schema，普通租户保留共享表 |
| S3 | 独立实例（专属租户） | 企业版大客户/合规要求高 | 按租户或租户组独立部署 |

### 1.2 当前建议
- 先落地 S1（行级隔离）作为主干方案。
- 架构上预留 S2/S3：
  - 数据源路由抽象（`TenantDataSourceRouter` 预留）。
  - 租户元数据中心维护 `db_mode/shared_db/schema/dedicated`。

---

## 2. 租户数据隔离方案

### 2.1 隔离层次
1. 应用层隔离：JWT 中携带 `tid`，请求上下文强制注入。
2. ORM 层隔离：MyBatis-Plus 租户拦截器（后续启用）统一追加 `tenant_id` 条件。
3. 数据层隔离：所有业务表包含 `tenant_id`，建立复合索引。
4. 运维层隔离：大客户可升级到独立 schema/实例。

### 2.2 表级策略

| 表类型 | 示例 | 隔离策略 |
|---|---|---|
| 业务数据表 | inventory, inbound_orders, sales_orders | 必须 `tenant_id` 强隔离 |
| 组织身份表 | users, roles, user_roles, role_permissions | 必须 `tenant_id` 强隔离 |
| 平台公共字典 | permissions（全局权限点） | 可无 `tenant_id`，只读 |
| 租户主数据 | tenants | 平台级管理，限制访问 |

### 2.3 防串租兜底
- 每个查询默认附加 `tenant_id = RequestContext.tenantId()`。
- 管理员跨租户能力仅限平台角色，且必须留审计日志。
- Redis Key 强制带租户前缀：`{tenantId}:module:key`。

---

## 3. `tenant_id` 使用规范

### 3.1 写入规范
- 新增数据时必须显式写入 `tenant_id`。
- 禁止客户端直接传 `tenant_id`，以后端 JWT 解析为准。

### 3.2 查询规范
- 所有业务查询必须带 `tenant_id` 条件。
- 复合唯一键优先采用 `(tenant_id, business_code)`。
- 复合索引优先 `(tenant_id, status, created_at)`。

### 3.3 更新/删除规范
- `UPDATE/DELETE` 必须包含 `tenant_id` 条件。
- 逻辑删除记录保留 `tenant_id` 便于审计与恢复。

### 3.4 审计规范
- `audit_logs` 记录 `tenant_id`、操作人、资源ID、前后值。

---

## 4. 权限与角色如何按租户隔离

### 4.1 RBAC 模型
- `permissions`：平台级权限点定义（全局）。
- `roles`：租户内角色模板（按 `tenant_id`）。
- `users`：租户内用户。
- `user_roles`、`role_permissions`：租户内绑定关系。

### 4.2 权限边界

| 角色类型 | 作用域 | 说明 |
|---|---|---|
| 平台管理员（Platform Admin） | 全平台 | 管租户开通、套餐、计费、全局监控 |
| 租户管理员（Tenant Admin） | 单租户 | 管本租户用户、角色、配置、配额 |
| 业务角色（仓储/采购/销售/客服） | 单租户 | 仅业务模块权限 |

### 4.3 数据权限建议
- 在角色上增加 `data_scope`（ALL/WAREHOUSE/CUSTOM）。
- 高级版支持按仓库、部门、渠道做数据范围限制。

---

## 5. 租户配置中心设计

### 5.1 目标
- 每个租户可独立配置业务策略与系统行为。
- 配置热更新，避免频繁重启服务。

### 5.2 配置分层

| 层级 | 内容 | 存储 |
|---|---|---|
| 平台默认配置 | 全局默认阈值、功能开关 | `application.yml` |
| 租户覆盖配置 | 低库存阈值、单据编号规则、客服SLA、AI路由 | `tenant_settings`（建议新增） |
| 运行态缓存 | 高频配置读取 | Redis |

### 5.3 建议新增表
- `tenant_settings`：`tenant_id, config_group, config_key, config_value, value_type, updated_at`。
- `tenant_features`：`tenant_id, feature_code, enabled, quota_limit`。

---

## 6. 套餐设计建议

### 6.1 套餐维度
- 用户数上限
- 仓库数上限
- 月单据量（入库/出库/销售等）
- 报表能力（基础/高级）
- API 与集成能力
- AI 调用额度与模型等级
- 客服坐席数与高级功能

### 6.2 套餐模型建议表
- `plans`：套餐定义（价格、周期、基础额度）。
- `plan_features`：套餐功能矩阵。
- `tenant_subscriptions`：租户订阅关系（生效/到期/状态）。

---

## 7. 免费版 / 专业版 / 企业版建议

| 版本 | 目标客户 | 能力范围 | 限制建议 |
|---|---|---|---|
| 免费版 | 小团队试用 | WMS基础 + 基础ERP + Mock/Rule AI + 基础客服 | 用户<=3，仓库<=1，月单据<=1000 |
| 专业版 | 成长型商家 | 全量业务闭环 + 高级报表 + 工单升级 + AI增强 | 用户<=30，仓库<=10，含AI额度 |
| 企业版 | 集团/大客户 | 多组织、多仓、专属支持、独立部署可选 | 可定制额度与SLA |

---

## 8. AI 增值包设计建议

### 8.1 产品分层
- 基础 AI（免费）：Mock/Rule、FAQ问答、库存分析模板。
- 标准 AI 包：更高额度、日报周报、客服建议回复。
- 高级 AI 包：Paid LLM、复杂经营建议、对话总结、质检。

### 8.2 计费维度
- 调用次数（requests）
- Token 消耗（paid provider）
- 高价值任务（报表总结、批量分析）

### 8.3 技术约束
- 统一走 `AIProvider` 抽象层。
- `app.ai.paid-enabled` 按租户可控（平台开关 + 租户开关双重校验）。

---

## 9. 计费与调用量统计建议

### 9.1 计量对象

| 计量项 | 统计口径 |
|---|---|
| 月活用户数 | 当月有登录行为的去重用户 |
| 单据处理量 | 入库/出库/采购/销售单据总量 |
| API 调用量 | 按租户、模块、状态码聚合 |
| AI 调用量 | provider、scene、request/token/latency |
| 客服会话量 | 新建会话、消息量、转人工率 |

### 9.2 建议新增表
- `usage_meter_daily`：按天沉淀核心用量。
- `billing_invoices`：账单与开票状态。
- `billing_invoice_items`：账单明细项（基础费、增值费、超额费）。

### 9.3 Redis 用法
- 实时计数（当日）：`usage:{tenantId}:{metric}:{yyyyMMdd}`。
- 每日定时落库后清理/过期。

---

## 10. 商用部署建议

### 10.1 部署分层
- 控制平面（平台后台）：租户管理、套餐、计费、运营。
- 业务平面（租户业务）：WMS/ERP/AI/客服。

### 10.2 部署模式
1. 共享部署（默认）：一套服务承载多租户。
2. 专属部署（企业版）：租户独立 compose 或独立集群。

### 10.3 Docker 化建议
- 使用同一镜像，多环境变量驱动。
- 租户配置存 DB + Redis，不放在镜像内。
- 支持租户级密钥配置（AI、Webhook、对象存储）通过环境变量或密钥管理注入。

---

## 11. 风控与安全建议

### 11.1 数据安全
- 强制租户行级隔离。
- 敏感字段脱敏展示（手机号、地址、联系人）。
- 关键数据传输/存储加密（密码哈希、密钥隔离）。

### 11.2 访问安全
- JWT 短期有效 + 刷新机制。
- 平台管理接口启用更严格鉴权（MFA 后续可加）。
- 接口限流（按租户、IP、用户三层）。

### 11.3 商业风控
- 套餐超额策略：告警 -> 限流 -> 升级引导。
- 欠费策略：只读降级（保留导出与续费入口），不直接删数据。
- AI 成本风控：高成本模型需租户白名单 + 配额阈值。

---

## 12. 从单租户平滑升级到多租户的落地路径

### 阶段 1：单租户规范化（当前）
- 保持 `tenant_id=1` 默认运行。
- 所有新功能继续严格带 `tenant_id`。

### 阶段 2：认证链路多租户化
- 登录按 `tenantCode` 识别租户。
- JWT 写入 `tid`。
- 请求上下文统一取租户ID。

### 阶段 3：ORM 自动隔离
- 启用 MyBatis-Plus TenantLineInnerInterceptor。
- 清理遗漏的手动条件，保留关键 SQL 白名单。

### 阶段 4：套餐与计量上线
- 上线 `plans/subscriptions/usage_meter`。
- 在网关或应用层增加租户配额检查。

### 阶段 5：大客户专属部署
- 支持租户路由到独立 schema/实例。
- 平台统一运营，租户数据物理隔离。

---

## Spring Boot + Docker 配置组织建议

### 1. 配置层级
- `application.yml`：全局默认与平台级开关。
- `application-dev.yml`：本地开发。
- `application-docker.yml`：容器环境连接（mysql/redis 主机名）。
- `tenant_settings`：租户运行时配置覆盖。

### 2. 建议环境变量

| 变量 | 用途 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | 环境选择 |
| `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` | MySQL连接 |
| `REDIS_HOST/REDIS_PORT/REDIS_DB` | Redis连接 |
| `JWT_SECRET` | 签名密钥 |
| `AI_PROVIDER` | 默认AI provider |
| `AI_PAID_ENABLED` | 是否启用付费AI |
| `APP_TENANT_MODE` | `single` / `multi`（建议新增） |
| `APP_BILLING_ENABLED` | 是否开启计费（建议新增） |

### 3. Docker Compose 建议
- 本地默认 `APP_TENANT_MODE=single`。
- 预发/商用可切 `multi` 并启用套餐与计费校验。
- 使用同一套编排，差异由环境变量和数据库配置决定。

---

## 本阶段最小实现建议（不大改核心模块）

1. 文档与配置先行：先完成本方案文档与配置约束。
2. 新增最小平台表（后续迭代）：`plans`, `tenant_subscriptions`, `tenant_settings`。
3. 在 `settings` 模块增加“租户配置读取”接口（仅读，不改核心流程）。
4. 在登录流程中保留 `tenantCode` 校验扩展点（当前已具备字段）。

这能保持现有单租户稳定运行，同时为后续商业化提供最短升级路径。
