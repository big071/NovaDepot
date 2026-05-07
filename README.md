# NovaDepot

> **版本：v1.1 "Closed Loop"**
> **发布日期：2026-05-07**
> **验收状态：E2E 20/20 全绿 + 后端测试 8/8 + 数据质量检查通过**

---

## 1. 项目定位

NovaDepot 是一款面向小团队（3-15 人）的**智能仓储管理系统 + 轻量进销存 ERP + AI 助手 + 智能客服**一体化平台。

v1.1 在 v1.0 的 WMS、客服、AI、Agent、知识库和审计基础上，补齐采购、销售、应收应付、盘点、CSV 导入、打印和备份能力，让系统从“智能仓库操作台”升级为“轻量进销存一体化系统”。

当前仍是**单租户、本地 Docker 一键启动**架构，已预留 `tenant_id` 字段和一致性检查，但不启用多租户过滤。

---

## 2. 核心能力

| 领域 | 能力 |
|---|---|
| 仓储运营 | 库存总览、低库存预警、手工入库、手工出库、库存流水、库存不足拦截 |
| 轻量 ERP | 往来单位、采购单、销售单、采购转入库草稿、销售转出库草稿 |
| 进销存闭环 | 采购 -> 入库 -> 库存 -> 出库 -> 销售，WMS 单据可追溯 ERP 来源 |
| 轻量资金台账 | 应付台账、应收台账、付款登记、收款登记、余额和状态 |
| 库存盘点 | 盘点单、实盘录入、差异复核、库存调整流水 |
| 运营收口 | 产品/库存/往来单位 CSV 导入、错误报告、入库/出库/拣货打印、数据备份 |
| 客服与工单 | 多会话管理、AI 建议/自动回复、工单创建/流转/指派、处理历史时间线、工单沉淀知识 |
| 知识库 | FAQ 维护、SOP 维护、规则配置 |
| AI 助手 | 企业级 AI 对话、意图识别、知识引用展示 |
| Agent 中心 | 低库存分析、补货建议、异常巡检、执行历史回溯 |
| 审计中心 | ERP、WMS、财务、盘点、导入、备份、客服、知识库操作审计 |
| RBAC 权限 | 4 角色体系 + 页面级 + 操作级权限控制 + 统一 403 |

---

## 3. v1.1 相比 v1.0 的变化

v1.0 交付的是智能仓库操作台，v1.1 增加了轻量进销存闭环和运营刚需。

| 领域 | v1.0 | v1.1 |
|---|---|---|
| WMS | 手工入库、出库、库存流水 | 保留手工流程，并支持 ERP 来源追溯 |
| ERP | 无完整采购/销售链路 | 新增往来单位、采购单、销售单 |
| 进销存 | WMS 单链路 | 打通采购、入库、库存、出库、销售 |
| 财务可见性 | 无轻量台账 | 新增应付、应收、付款、收款 |
| 盘点 | 无库存盘点 | 新增盘点和库存调整流水 |
| 数据迁入 | 依赖 seed 和人工维护 | 新增 CSV 导入和错误报告 |
| 打印 | 无纸面模板 | 新增入库、出库、拣货浏览器打印 |
| 备份 | 基础脚本 | 增强备份脚本和备份记录 |
| 多租户 | 单租户 | 仅预留 `tenant_id`，不启用过滤 |

---

## 4. 技术栈

| 层 | 技术 |
|---|---|
| Frontend | Vue 3 + Vite + TypeScript + Vue Router + Pinia + Tailwind CSS + Naive UI + ECharts |
| Backend | Java 17 + Spring Boot 3 + MyBatis-Plus |
| Database | MySQL 8 (utf8mb4) |
| Cache | Redis |
| Auth | Spring Security + JWT + RBAC |
| API Docs | SpringDoc OpenAPI / Swagger |
| E2E | Playwright (TypeScript) |
| Deployment | Docker + Docker Compose |

---

## 5. 快速启动

```powershell
docker compose up -d mysql redis backend frontend-vue
```

访问地址：

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost:3100 |
| 后端 API | http://localhost:18080 |
| Swagger | http://localhost:18080/swagger-ui/index.html |

环境变量：

- Vue 示例：`frontend-vue/.env.example`
- Docker Compose 默认使用：`VITE_API_BASE_URL=http://localhost:18080/api/v1`

---

## 6. 演示账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `admin123` | 管理员 | 全部权限，含审计、备份、财务登记和盘点确认 |
| `warehouse01` | `pass123` | 仓储运营 | WMS、采购转入库、库存导入、盘点操作 |
| `cs01` | `pass123` | 客服运营 | 客服、销售、应收查看、往来单位导入 |
| `observer01` | `pass123` | 观察员 | 只读访问 |

---

## 7. 常用命令

重置数据到干净商业基线：

```powershell
./scripts/ops/reset-commercial-baseline.ps1
```

数据质量检查：

```powershell
./scripts/ops/data-quality-check.ps1
```

前端检查和 E2E：

```bash
cd frontend-vue
npm run typecheck
npm run build
npm run test:e2e
```

后端测试：

```bash
cd backend
mvn test
```

数据库备份：

```powershell
./scripts/ops/backup.ps1
```

Linux / Docker 环境可使用：

```bash
./scripts/ops/backup.sh
```

---

## 8. v1.1 进销存闭环

1. 维护往来单位。
2. 创建并确认采购单。
3. 从采购单生成入库草稿。
4. 入库单继续走 WMS 提交、审核、过账流程。
5. 入库过账后库存增加，采购单已收数量和状态联动。
6. 创建并确认销售单。
7. 从销售单生成出库草稿，生成前检查库存。
8. 出库单继续走 WMS 提交、审核、发运流程。
9. 出库发运后库存减少，销售单已发数量和状态联动。
10. 采购和销售形成应付/应收台账，可手工登记付款/收款。

采购/销售转 WMS 只生成 `DRAFT` 草稿，不自动过账或发运。v1.0 手工入库/出库流程仍然保留。

---

## 9. 运营能力说明

- 盘点：`/wms/stock-take` 支持盘点单、实盘录入、差异复核和库存调整。
- CSV：只支持 UTF-8 CSV，不支持 Excel；正确行导入，错误行跳过并生成报告。
- 打印：使用浏览器打印和 `@media print`，支持 A4 入库单、出库单和拣货单。
- 备份：使用 mysqldump + gzip 写入 `./backups/`，管理员可查看备份记录。
- tenant_id：只做字段预留和一致性检查，不启用 TenantInterceptor 或租户过滤。

---

## 10. 已知限制

| 限制 | 说明 |
|---|---|
| 单租户运行 | `tenant_id` 已预留，但当前不做租户隔离 |
| 轻量财务 | 应收应付是台账，不是总账、明细账或完整财务系统 |
| CSV only | 不支持 Excel `.xlsx`、导入回滚或覆盖导入 |
| 浏览器打印 | 不支持批量打印、标签打印机或模板设计器 |
| 本地备份 | 不支持自动恢复、云备份、异地备份或增量备份 |
| AI Provider | 保持 Mock + Rule Provider 和抽象层，v1.1 不接入 DeepSeek 正式生产能力 |
| 无退货/发票 | 不支持采购退货、销售退货、发票、合同、报价、询比价 |
| Docker Hub 网络 | 若基础镜像 token/metadata 拉取超时，需要网络恢复后重试前端镜像构建 |

---

## 11. 后续路线

| 版本 | 目标 |
|---|---|
| v1.2 | DeepSeek / 付费 AI Provider 正式接入、智能分析增强、报表和通知增强 |
| v2.0 | 多租户正式启用、计费、商业化平台运营 |

---

## 12. 文档入口

完整文档索引见 [docs/README.md](docs/README.md)。

v1.1 发布文档：

- [v1.1 Release Notes](docs/90-v1.1-release-notes.md)
- [v1.1 Delivery Notes](docs/91-v1.1-delivery-notes.md)
- [v1.1 Acceptance Checklist](docs/92-v1.1-acceptance-checklist.md)

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
