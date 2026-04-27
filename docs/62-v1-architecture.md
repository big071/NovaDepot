# NovaDepot v1.0 技术架构

## 1. 架构概览

NovaDepot v1.0 采用 **前后端分离 + 模块化单体后端 + Docker Compose 本地部署** 的架构。

```
┌─────────────────────────────────────────────────────────┐
│                    Nginx (frontend-vue)                 │
│                http://localhost:3100                     │
│            Vue 3 + Vite + Naive UI + ECharts            │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP REST /api/v1/*
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot 3 (backend)                    │
│                http://localhost:18080                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ Security  │ │  Modules │ │Common   │ │Repository│   │
│  │ JWT+RBAC │ │  (业务模块)│ │ (工具类) │ │(MyBatis+) │   │
│  └──────────┘ └──────────┘ └──────────┘ └────┬─────┘   │
└──────────────────────────────────────────────┼──────────┘
                         │                     │
              ┌──────────┴──────┬──────────────┘
              ▼                 ▼
     ┌───────────┐     ┌───────────┐
     │  MySQL 8  │     │   Redis   │
     │  :3306    │     │  :6379    │
     └───────────┘     └───────────┘
```

## 2. 技术栈明细

| 层次 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + Composition API | 3.x |
| 构建工具 | Vite | 5.x |
| 状态管理 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| UI 组件库 | Naive UI | 2.x |
| CSS 框架 | Tailwind CSS | 3.x |
| 图表 | ECharts | 5.x |
| HTTP 客户端 | Axios | 1.x |
| 后端框架 | Spring Boot 3 | 3.x |
| 语言 | Java | 17 |
| ORM | MyBatis-Plus | 3.x |
| 安全 | Spring Security + JWT | - |
| API 文档 | SpringDoc OpenAPI | 2.x |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 测试 | JUnit 5 + Playwright | - |
| 部署 | Docker + Docker Compose | - |

## 3. 后端模块结构

```
com.novadepot.backend
├── model/entity/           # 数据实体（与数据库表 1:1 映射）
├── repository/             # MyBatis-Plus Mapper 接口
├── modules/                # 业务模块
│   ├── agent/              # Agent 中心
│   ├── ai/                 # AI 助手（含 Provider 抽象）
│   ├── auditlogs/          # 审计日志
│   ├── auth/               # 认证与鉴权
│   ├── customerservice/    # 客服系统
│   ├── inboundorders/      # 入库管理
│   ├── inventory/          # 库存管理
│   ├── knowledge/          # 知识库（FAQ/SOP/规则）
│   ├── locations/          # 库位管理
│   ├── notifications/      # 通知
│   ├── outboundorders/     # 出库管理
│   ├── permissions/        # 权限管理
│   ├── products/           # 产品管理
│   ├── reports/            # 报表
│   ├── roles/              # 角色管理
│   ├── settings/           # 系统设置
│   ├── users/              # 用户管理
│   └── warehouses/         # 仓库管理
├── security/               # 安全配置
│   ├── jwt/                # JWT 令牌服务
│   └── permission/         # 权限注解与 AOP
└── common/                 # 公共组件
    ├── api/                # 统一响应格式
    ├── config/             # 配置类
    ├── enums/              # 枚举常量
    └── exception/          # 全局异常处理
```

## 4. 前端模块结构

```
frontend-vue/src/
├── pages/                  # 页面组件
│   ├── ai/                 # AI 助手 (/ai/enterprise)
│   ├── agent/              # Agent 中心 (/agent/center)
│   ├── cs/                 # 客服工作台 (/cs/workspace) + 知识维护
│   ├── dashboard/          # 仪表盘 (/dashboard)
│   ├── system/             # 审计中心 (/system/audit-center) + 用户管理
│   └── wms/                # 库存 (/wms/inventory) + 入库/出库
├── services/               # API 服务层
│   ├── api.ts              # Axios 实例
│   ├── agent.ts            # Agent API
│   ├── auth.ts             # 认证 API
│   ├── customerService.ts  # 客服 API
│   └── knowledge.ts        # 知识库 API
├── stores/                 # Pinia 状态管理
│   └── auth.ts             # 认证状态
├── router/                 # 路由配置（含角色守卫）
└── components/             # 公共组件
```

## 5. 核心数据表

| 表名 | 模块 | 说明 |
|------|------|------|
| `users` | auth | 用户 |
| `roles` | auth | 角色 |
| `user_roles` | auth | 用户-角色关联 |
| `permissions` | auth | 权限码 |
| `role_permissions` | auth | 角色-权限关联 |
| `inventory` | wms | 库存 |
| `inventory_transactions` | wms | 库存事务流水 |
| `inbound_orders` | wms | 入库单 |
| `inbound_order_items` | wms | 入库单明细 |
| `outbound_orders` | wms | 出库单 |
| `outbound_order_items` | wms | 出库单明细 |
| `customer_service_sessions` | cs | 客服会话 |
| `customer_service_messages` | cs | 客服消息 |
| `customer_service_tickets` | cs | 客服工单 |
| `faq_knowledge` | knowledge | FAQ 知识 |
| `sop_knowledge` | knowledge | SOP 知识 |
| `rule_config` | knowledge | 规则配置 |
| `ai_conversations` | ai | AI 会话 |
| `ai_messages` | ai | AI 消息 |
| `agent_task_runs` | agent | Agent 任务执行记录 |
| `audit_logs` | audit | 审计日志 |

## 6. 服务端口

| 服务 | 端口 | 访问地址 |
|------|------|---------|
| Vue 前端 (Nginx) | 3100 | http://localhost:3100 |
| Spring Boot API | 18080 | http://localhost:18080 |
| Swagger UI | 18080 | http://localhost:18080/swagger-ui/index.html |
| MySQL | 3306 | 内部 |
| Redis | 6379 | 内部 |

## 7. AI Provider 抽象层

v1.0 已建立 `AiProvider` 接口，当前实现：

| Provider | 用途 | 说明 |
|----------|------|------|
| `MockAiProvider` | 开发/测试 | 返回固定模板回复 |
| `RuleAiProvider` | 规则驱动 | 基于 FAQ/SOP/规则匹配生成回复 |
| `DeepSeekChatAiProvider` | 付费模型（预留） | DeepSeek Chat API |
| `DeepSeekReasonerAiProvider` | 付费模型（预留） | DeepSeek Reasoner API |
| `PaidAiProvider` | 付费抽象层（预留） | 未来付费模型的统一封装 |

当前默认使用 `RuleAiProvider`，所有业务代码通过 `AiProvider` 接口调用，切换到付费模型只需修改配置。

## 8. 安全架构

```
用户请求 → JwtAuthFilter（验证 Token）
         → SecurityConfig（角色/权限校验）
         → RequirePermission 注解 + PermissionAspect（AOP 方法级权限）
         → Controller → Service → Mapper → DB
         → AuditLogRecordService（自动记录审计日志）
```

- 认证方式：用户名 + 密码 → 返回 JWT Token
- 授权方式：RBAC（角色 → 权限码）
- 权限粒度：页面级（路由守卫）+ 操作级（按钮/API 权限）
- 越权处理：统一返回 403 状态码 + 前端 403 页面提示