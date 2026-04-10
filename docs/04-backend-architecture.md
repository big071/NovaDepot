# NovaDepot 后端架构设计（Java 17 + Spring Boot 3）

## 1. 后端整体架构设计

## 1.1 架构目标
- 前期采用“模块化单体（Modular Monolith）”提升交付效率。
- 模块边界清晰，后期可按业务域拆分微服务。
- 突出库存流水、单据状态流转、权限、审计日志、AI Provider 抽象、客服工作台。

## 1.2 架构分层
- 接口层：`controller`（REST API + OpenAPI 注解）
- 应用层：`application/service`（用例编排、事务控制、权限校验入口）
- 领域层：`domain`（状态机、业务规则、库存计算）
- 基础设施层：`infrastructure`（MyBatis-Plus、Redis、文件存储、消息、第三方适配）

## 1.3 核心设计原则
- 单据主表+明细表模型，统一状态机驱动流转。
- 库存变更只通过库存服务入口，强制写库存流水。
- 业务代码不得直接依赖 AI 厂商 SDK，统一走 `AIProvider` 抽象。
- 所有关键写操作必须留审计日志。

---

## 2. 模块划分建议

| 模块 | 说明 |
|---|---|
| `novadepot-auth` | 认证、JWT、登录会话 |
| `novadepot-iam` | RBAC、数据权限、角色资源 |
| `novadepot-tenant` | 多租户预留（单租户模式可降级） |
| `novadepot-masterdata` | 商品、分类、单位、仓库、库位、供应商、客户 |
| `novadepot-wms-inventory` | 库存余额、库存流水、库存预警 |
| `novadepot-wms-inbound` | 入库单与收货上架 |
| `novadepot-wms-outbound` | 出库单、拣货复核、发运 |
| `novadepot-wms-transfer` | 调拨单（调出调入） |
| `novadepot-wms-stocktake` | 盘点任务、差异处理 |
| `novadepot-erp-purchase` | 采购单与到货协同 |
| `novadepot-erp-sales` | 销售单与退货协同 |
| `novadepot-report` | 报表聚合、统计查询、导出 |
| `novadepot-notification` | 站内通知、消息订阅、渠道适配 |
| `novadepot-audit` | 审计日志、操作轨迹 |
| `novadepot-file` | 文件上传、对象存储抽象 |
| `novadepot-ai` | AI 编排、Provider 抽象、模板管理 |
| `novadepot-cs` | 智能客服会话、工单、规则、FAQ |
| `novadepot-job` | 定时任务、补偿任务 |
| `novadepot-common` | 公共响应、异常、工具、常量 |

---

## 3. 目录结构建议

```text
novadepot-backend/
  pom.xml
  novadepot-boot/
    src/main/java/com/novadepot/boot/
      NovaDepotApplication.java
      config/
  novadepot-common/
  novadepot-auth/
  novadepot-iam/
  novadepot-tenant/
  novadepot-masterdata/
  novadepot-wms-inventory/
  novadepot-wms-inbound/
  novadepot-wms-outbound/
  novadepot-wms-transfer/
  novadepot-wms-stocktake/
  novadepot-erp-purchase/
  novadepot-erp-sales/
  novadepot-report/
  novadepot-notification/
  novadepot-audit/
  novadepot-file/
  novadepot-ai/
  novadepot-cs/
  novadepot-job/
  deploy/
    docker-compose.yml
    mysql/init/
    backend/.env
```

---

## 4. 每个模块职责边界

| 模块 | 只负责 | 不负责 |
|---|---|---|
| `wms-inventory` | 库存增减、锁定、流水记录 | 单据审批逻辑 |
| `wms-inbound/outbound` | 单据业务规则、状态推进 | 直接修改库存表 |
| `iam` | 权限判定与数据范围 | 业务状态校验 |
| `ai` | 场景编排与模型适配 | 直接处理业务单据 |
| `cs` | 会话/工单/知识库 | 库存计算与财务逻辑 |

约束：库存表写入必须通过 `inventory domain service`，禁止旁路更新。

---

## 5. `controller/service/mapper/entity/dto/vo` 划分建议

| 层 | 说明 |
|---|---|
| `controller` | 仅处理入参校验、调用应用服务、返回统一响应 |
| `service` | 用例编排、事务、状态机驱动、跨模块协调 |
| `mapper` | MyBatis-Plus 数据访问，避免业务逻辑 |
| `entity` | 表结构映射对象，尽量保持贫血 |
| `dto` | 请求参数与应用层传输对象 |
| `vo` | 对外返回对象（列表行、详情页聚合） |

建议：复杂查询使用 `query object + custom mapper xml`，避免把复杂 SQL 放在 service。

---

## 6. 公共模块设计

`novadepot-common` 建议包含：
- `BaseEntity`（`id/tenantId/createdAt/createdBy/updatedAt/updatedBy/deleted`）
- `ApiResponse<T>`、分页模型 `PageResult<T>`
- 统一异常体系 `BizException`
- 错误码枚举 `ErrorCode`
- 上下文 `UserContext`、`TenantContext`
- 通用工具（时间、金额、校验、脱敏）

---

## 7. 权限认证模块设计

### 7.1 认证（Spring Security + JWT）
- 登录成功签发 Access Token + Refresh Token。
- JWT 内最少包含：`userId`、`tenantId`、`roles`、`jti`。
- Redis 存储会话黑名单/白名单，支持强制下线。

### 7.2 授权（RBAC + 数据权限）
- 接口权限：注解 + AOP 统一校验（如 `@RequirePermission`）。
- 数据权限：按仓库、组织、租户自动拼接查询条件。
- 超管绕过机制仅限平台级角色。

---

## 8. 审计日志模块设计

| 维度 | 设计 |
|---|---|
| 触发点 | 登录、权限变更、状态流转、库存变更、导入导出、配置变更 |
| 记录字段 | actor、action、resource、before/after、ip、ua、time |
| 实现方式 | AOP + 业务显式日志结合 |
| 存储策略 | 在线 90 天 + 归档表/冷存储 |

---

## 9. 通知模块设计

- 统一通知模型：业务事件 -> 通知任务 -> 渠道发送。
- MVP 渠道：站内信。
- 后续渠道：邮件、Webhook、短信。
- 通知状态：`PENDING/SENT/FAILED/READ`。
- 未读计数放 Redis，落库以 `notifications` 为准。

---

## 10. 文件上传模块设计

- 统一 `file` 模块管理上传、下载签名、元数据。
- 存储抽象：`StorageProvider`（本地、MinIO、S3）。
- 业务表只存 `file_id` 或 `object_key`，不直接存二进制。
- 安全：鉴权下载、文件大小与类型白名单、病毒扫描预留。

---

## 11. 报表模块设计

- OLTP 库实时聚合 + Redis 短缓存（MVP）。
- 复杂报表改异步任务生成（导出、月报）。
- 报表查询接口和业务写接口隔离（读写分离预留）。
- 统一查询参数：时间区间、仓库、商品、客户、供应商。

---

## 12. AI 模块设计

### 12.1 核心组件
- `AIOrchestrator`：按场景路由。
- `AIProvider`：统一接口（`chat/analyze/suggest/health`）。
- `PromptTemplateService`：模板版本化。
- `AIGuardService`：权限、敏感信息过滤。

### 12.2 Provider 分层
- `MockProvider`（开发期）
- `RuleProvider`（规则与模板）
- `PaidLLMProvider`（商用期）

关键原则：业务模块只能调用 `AIOrchestrator`。

---

## 13. 智能客服模块设计

- 会话模型：`session -> messages -> ticket(optional)`。
- 客服工作台接口需要聚合：客户信息、订单、库存、历史会话、建议回复。
- 规则引擎：超时升级、缺货话术、延迟发货提示。
- FAQ 与知识库统一管理，支持版本与启停。

---

## 14. 多租户预留方案

前期：单库单实例 + 所有业务表 `tenant_id`。

后期演进：
1. 行级隔离（当前方案）
2. 按租户分 schema（高价值租户）
3. 独立实例（大客户专属）

MyBatis-Plus：启用租户拦截器自动注入 `tenant_id` 条件。

---

## 15. Redis 使用建议

| 场景 | Key |
|---|---|
| 登录态 | `auth:token:{tenantId}:{userId}:{jti}` |
| 权限缓存 | `iam:perm:{tenantId}:{userId}` |
| 库存锁 | `inv:lock:{tenantId}:{warehouseId}:{productId}` |
| 幂等键 | `idem:{tenantId}:{requestId}` |
| 通知未读数 | `notify:unread:{tenantId}:{userId}` |
| AI 限流 | `ai:quota:{tenantId}:{date}` |

---

## 16. 定时任务建议

- 库存预警扫描（分钟级）
- 工单 SLA 超时检查（分钟级）
- 报表缓存预热（日级/小时级）
- 导入导出失败重试（分钟级）
- 审计归档与日志清理（日级）

建议使用 Spring Scheduling + 分布式锁（Redis）防重复执行。

---

## 17. 错误处理与异常码规范

- 统一业务异常：`BizException(code, message)`。
- 错误码分段：
  - `AUTH-xxxx` 认证授权
  - `WMS-xxxx` 仓储
  - `ERP-xxxx` 采购销售
  - `AI-xxxx` 智能
  - `CS-xxxx` 客服
  - `SYS-xxxx` 系统
- 统一全局异常处理：`@RestControllerAdvice`。

---

## 18. API 响应结构规范

```json
{
  "code": "0",
  "message": "success",
  "data": {},
  "traceId": "xxx",
  "timestamp": "2026-04-10T16:00:00+08:00"
}
```

分页返回：
- `data.list`
- `data.pageNo`
- `data.pageSize`
- `data.total`

---

## 19. 日志与监控建议

- 日志：JSON 结构化日志（应用日志、审计日志分离）。
- Trace：引入 `traceId`，贯穿请求全链路。
- 指标：QPS、响应时长、错误率、库存写入冲突率、任务失败率。
- 健康检查：`/actuator/health` + DB + Redis 可用性。

---

## 20. 商用部署建议

- 分环境：`dev`、`staging`、`prod`。
- 镜像版本化：`novadepot-backend:{version}`。
- 配置中心可后续接入，前期用环境变量 + yml 分层。
- 预留横向扩展：无状态 API 服务 + 外部 Redis + MySQL 主从。

---

## 21. Docker 化部署建议

## 21.1 `application.yml` 分环境策略
- `application.yml`：公共配置（日志级别、Jackson、线程池）
- `application-dev.yml`：本机开发（localhost 连接）
- `application-docker.yml`：容器网络（服务名连接）

示例（容器环境）：
- `spring.datasource.url=jdbc:mysql://mysql:3306/novadepot?...`
- `spring.data.redis.host=redis`

## 21.2 容器环境变量建议
- `SPRING_PROFILES_ACTIVE=docker`
- `DB_HOST=mysql` `DB_PORT=3306` `DB_NAME=novadepot`
- `DB_USERNAME=root` `DB_PASSWORD=***`
- `REDIS_HOST=redis` `REDIS_PORT=6379`
- `JWT_SECRET=***`

---

## 22. Docker Compose 本地编排建议

## 22.1 编排关系（本地桌面）
- `frontend`（Next.js）依赖 `backend`
- `backend`（Spring Boot）依赖 `mysql`、`redis`
- `mysql` 挂载初始化脚本与数据卷
- `redis` 挂载配置与数据卷（可选）

## 22.2 结构示意
```yaml
services:
  frontend:
    ports: ["3000:3000"]
    depends_on: [backend]
  backend:
    ports: ["8080:8080"]
    depends_on: [mysql, redis]
  mysql:
    ports: ["3306:3306"]
    volumes:
      - ./mysql/init:/docker-entrypoint-initdb.d
  redis:
    ports: ["6379:6379"]
```

## 22.3 访问路径规划
- 前端：`http://localhost:3000`
- 后端 API：`http://localhost:8080/api`
- Swagger：`http://localhost:8080/swagger-ui/index.html`

---

## A. 推荐的 Spring Boot 模块列表

- `novadepot-boot`
- `novadepot-common`
- `novadepot-auth`
- `novadepot-iam`
- `novadepot-tenant`
- `novadepot-masterdata`
- `novadepot-wms-inventory`
- `novadepot-wms-inbound`
- `novadepot-wms-outbound`
- `novadepot-wms-transfer`
- `novadepot-wms-stocktake`
- `novadepot-erp-purchase`
- `novadepot-erp-sales`
- `novadepot-report`
- `novadepot-notification`
- `novadepot-audit`
- `novadepot-file`
- `novadepot-ai`
- `novadepot-cs`
- `novadepot-job`

## B. 推荐的目录树

```text
novadepot-backend/
  pom.xml
  novadepot-boot/
  novadepot-common/
  novadepot-auth/
  novadepot-iam/
  novadepot-tenant/
  novadepot-masterdata/
  novadepot-wms-inventory/
  novadepot-wms-inbound/
  novadepot-wms-outbound/
  novadepot-wms-transfer/
  novadepot-wms-stocktake/
  novadepot-erp-purchase/
  novadepot-erp-sales/
  novadepot-report/
  novadepot-notification/
  novadepot-audit/
  novadepot-file/
  novadepot-ai/
  novadepot-cs/
  novadepot-job/
  deploy/
    docker-compose.yml
    mysql/init/
```

## C. 开发顺序建议

1. `common + auth + iam`（统一基座）
2. `masterdata + wms-inventory`（库存核心）
3. `wms-inbound/outbound/transfer/stocktake`（仓储闭环）
4. `erp-purchase + erp-sales`（轻 ERP 闭环）
5. `audit + notification + file`（平台能力）
6. `report + job`（统计与任务）
7. `ai + cs`（智能能力）
8. `tenant`（多租户增强）+ 商用化扩展

## 补充：脚手架运行文档
- 本轮后端脚手架落地与启动说明见：
  - `docs/08-backend-scaffold-and-run.md`
