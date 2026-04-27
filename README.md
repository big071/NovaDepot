# NovaDepot

> **版本：v1.0 "First Light"**  
> **交付日期：2026-04-27**  
> **测试状态：E2E 10/10 全绿 ✅ + 数据质量检查零报错 ✅**

---

## 1. 项目定位

NovaDepot 是一款面向小团队（3-15 人）的**智能仓储管理系统 + 轻量 ERP + AI 助手 + 智能客服**一体化平台。

当前阶段为**单租户、本地 Docker 一键启动**的最小闭环交付版，服务仓库管理员、仓储运营、客服运营、管理员四种角色协同工作。

---

## 2. 核心能力

| 领域 | 能力 |
|------|------|
| **仓储运营** | 库存总览、低库存预警、入库管理、出库管理、库存事务追踪 |
| **客服与工单** | 多会话管理、AI 建议/自动回复、工单创建/流转/指派、处理历史时间线、工单沉淀知识 |
| **知识库** | FAQ 维护、SOP 维护、规则配置（低库存阈值等） |
| **AI 助手** | 企业级 AI 对话、意图识别、知识引用展示 |
| **Agent 中心** | 低库存分析、补货建议、异常巡检、执行历史回溯 |
| **审计中心** | 操作审计日志、多维筛选（仅管理员可访问） |
| **RBAC 权限** | 4 角色体系 + 页面级 + 操作级权限控制 + 统一 403 |

---

## 3. 技术栈

| 层 | 技术 |
|----|------|
| Frontend | Vue 3 + Vite + TypeScript + Vue Router + Pinia + Tailwind CSS + Naive UI + ECharts |
| Backend | Java 17 + Spring Boot 3 + MyBatis-Plus |
| Database | MySQL 8 (utf8mb4) |
| Cache | Redis |
| Auth | Spring Security + JWT + RBAC |
| API Docs | SpringDoc OpenAPI / Swagger |
| E2E | Playwright (TypeScript) |
| Deployment | Docker + Docker Compose |

---

## 4. 快速启动

### 启动全部服务

```bash
docker compose up -d
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端 (Vue) | http://localhost:3100 |
| 后端 API | http://localhost:18080 |
| Swagger | http://localhost:18080/swagger-ui/index.html |

## 环境变量
- Vue 示例: `frontend-vue/.env.example`
- Docker Compose 中默认使用:
  - `VITE_API_BASE_URL=http://localhost:18080/api/v1`

---

## 5. 演示账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| `admin` | `admin123` | 管理员 | 全部权限，含审计中心、规则配置 |
| `warehouse01` | `pass123` | 仓储运营 | 入库/出库/库存操作 |
| `cs01` | `pass123` | 客服运营 | 客服会话、工单管理 |
| `observer01` | `pass123` | 观察员 | 只读访问 |

---

## 6. 操作命令

### 重置数据到干净基线

```powershell
./scripts/ops/reset-commercial-baseline.ps1
```

### 数据质量检查

```powershell
./scripts/ops/data-quality-check.ps1
```

### 前端 E2E 自动化测试

```bash
cd frontend-vue
npm run test:e2e
```

### 数据库备份

```powershell
./scripts/ops/backup.ps1
```

---

## 7. 关键目录

| 目录 | 说明 |
|------|------|
| `frontend-vue/` | Vue 3 前端 |
| `backend/` | Spring Boot 后端 |
| `backend/deploy/mysql/init/` | MySQL 初始化脚本 (schema + seed) |
| `backend/src/test/` | 后端单元测试 |
| `frontend-vue/tests/` | Playwright E2E 测试 |
| `scripts/ops/` | 运维脚本 (重置/备份/检查) |
| `docs/` | 项目文档 |

---

## 8. v1.0 已包含能力（完整清单）

- ✅ 库存总览、库存明细、库存流水
- ✅ 低库存预警与安全阈值配置
- ✅ 入库：创建 → 提交 → 审核 → 执行（过账）
- ✅ 出库：创建 → 提交 → 审核 → 发运
- ✅ 库存不足时发运失败并提示明确错误
- ✅ 库存事务追踪（来源单据类型与编号）
- ✅ 客服多会话管理、AI 建议/自动回复
- ✅ 工单创建、状态流转、责任人指派、备注
- ✅ 工单处理历史时间线
- ✅ 从工单沉淀 FAQ / SOP 草稿
- ✅ FAQ 维护：新增、编辑、启用、停用、草稿→确认
- ✅ SOP 维护：新增、编辑、启用、停用、草稿→确认
- ✅ 规则配置：管理员可改，非管理员只读
- ✅ AI 助手对话、意图识别、知识引用展示
- ✅ Agent 低库存分析、补货建议、异常巡检
- ✅ Agent 执行历史查询与回看
- ✅ 审计中心：知识/单据/客服操作审计、多维筛选
- ✅ 4 角色 RBAC：页面级 + 操作级权限 + 统一 403
- ✅ Docker 一键部署
- ✅ 数据质量自动化检查
- ✅ 10 条 Playwright E2E 用例全绿

---

## 9. v1.0 明确不包含

| 不包含 | 原因 |
|--------|------|
| 多租户正式版 | 先验证单租户业务闭环 |
| 计费系统 | 尚未进入商业化运营 |
| 完整 ERP 三链（采购/销售/财务） | 先 WMS 核心，ERP 后续扩展 |
| RAG / 向量数据库 | 当前 FAQ/SOP 规则匹配已足够 |
| 工作流引擎（BPM） | 单据状态机 + 审核已满足需求 |
| 知识图谱 | ROI 不足以当前阶段投入 |
| 付费 AI API 强依赖 | AI 层保持 Mock + Rule 优先 |
| 移动端 App | 先桌面端 Web |

---

## 10. 后续路线

| 版本 | 目标 |
|------|------|
| v1.1 | 采购/销售/财务简化 ERP 闭环 + 报表 + 通知 |
| v2.0 | 多租户 + 计费 + 付费 AI Provider + 平台化运营 |

---

## 11. 文档入口

完整文档索引见 [`docs/README.md`](docs/README.md)

v1.0 核心文档：
- [功能边界冻结](docs/61-v1-scope-and-boundary.md)
- [架构概述](docs/62-v1-architecture.md)
- [角色权限](docs/63-v1-roles-and-permissions.md)
- [核心业务流程](docs/64-v1-core-business-flows.md)
- [AI / Agent 能力](docs/65-v1-ai-agent-capabilities.md)
- [知识库 / FAQ / SOP / 规则](docs/66-v1-knowledge-faq-sop-rules.md)
- [Docker 部署](docs/67-v1-docker-deployment.md)
- [数据初始化与重置](docs/68-v1-data-init-and-reset.md)
- [测试与验收](docs/69-v1-test-and-acceptance.md)
- [演示走查](docs/70-v1-demo-walkthrough.md)
- [故障排查 FAQ](docs/71-v1-faq-troubleshooting.md)
