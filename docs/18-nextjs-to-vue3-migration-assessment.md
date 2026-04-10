# NovaDepot 前端从 Next.js 迁移到 Vue 3 评估方案

## 1. 评估范围与前提
- 本文仅做技术评估，不执行迁移代码改造。
- 目标是给出理性决策：是否应在当前阶段从 Next.js 迁移到 Vue 3。
- 约束遵循 AGENTS.md：分阶段交付、docs 先行、保持 Docker 本地可运行、避免无关重构。

## 2. 当前前端实现盘点（基于现仓库）

### 2.1 页面与路由
- 当前已实现 `11` 个页面（`app/**/page.tsx`）：
  - `/`
  - `/login`
  - `/dashboard`
  - `/wms/products`
  - `/wms/warehouses`
  - `/wms/locations`
  - `/wms/inventory`
  - `/wms/inbound`
  - `/wms/outbound`
  - `/ai/enterprise`
  - `/cs/workspace`
- 路由模式：Next.js App Router（目录即路由），含 `(auth)` 与 `(workspace)` 分组布局。

### 2.2 组件层
- 组件共 `12` 个：
  - `components/layout`：`5` 个（`app-shell/sidebar/topbar/theme-provider/theme-toggle`）
  - `components/ui`：`7` 个（`button/input/card/table/badge/page-header/simple-pagination`）
- UI 实现风格为轻量自定义组件 + Tailwind，非重依赖第三方完整 UI 框架。

### 2.3 状态管理
- 使用 Zustand（`store/ui-store.ts`），当前主要承载主题状态（`light/dark`）。
- 业务状态多数为页面级 `useState`，尚未形成复杂全局 store 体系。

### 2.4 API 调用层
- `lib/api/client.ts` 提供统一 `apiGet/apiPost` 与 Bearer Token 注入。
- Token 来源：`localStorage.novadepot-token`（登录页写入）。
- API Base 通过 `NEXT_PUBLIC_API_BASE_URL` 环境变量注入。

### 2.5 主题切换与设计系统
- ThemeProvider + ThemeToggle + CSS Variables（`globals.css`）。
- Tailwind 与设计令牌映射已落地（`tailwind.config.ts`）。
- 明暗主题已完成，且与组件体系耦合度适中。

### 2.6 Docker 部署方式
- 前端 Docker：多阶段 Node 镜像构建（`Dockerfile`）。
- 根编排：`docker-compose.yml`，前后端 + MySQL + Redis 一体运行。
- 当前前端联调环境变量使用 `NEXT_PUBLIC_API_BASE_URL=http://localhost:18080/api/v1`（浏览器可达）。

## 3. 迁移到 Vue 3 的目标栈建议

### 3.1 推荐技术栈（如决定迁移）
- Vue 3 + TypeScript
- Vite（构建与开发）
- Vue Router（路由）
- Pinia（状态管理）
- Tailwind CSS（沿用现有设计令牌）
- 组件库建议：
  - 首选：`Naive UI`（后台场景成熟、表格/表单能力强）
  - 备选：`Element Plus`（生态成熟）
  - 若强调复用现视觉：继续自建轻量组件（与当前模式最接近）

### 3.2 与 Spring Boot 后端对接变化
- API 协议基本不变：仍为 `/api/v1/*` + JWT Bearer。
- 变化主要在前端请求封装与拦截器（Axios 或 fetch 封装重写）。
- CORS、Docker 网络、环境变量机制仍可复用现方案。

## 4. 迁移成本评估

### 4.1 页面迁移成本
- 当前页面数量不大（11 页），基础迁移成本可控。
- 但 IA 文档规划远超现状（后续模块多），若现在切换框架，后续所有增量都要在新栈上实现并二次验证。
- 评估：`中等`（短期页面不多，长期机会成本高）。

### 4.2 组件迁移成本
- 现有组件数量不大（12 个），多数可 1:1 重写。
- 若引入 Naive UI，将发生视觉与交互一致性再设计成本。
- 若继续自建组件，迁移成本更低但初期产能受限。
- 评估：`中等偏低`。

### 4.3 状态管理迁移成本
- Zustand 当前使用深度浅，迁移到 Pinia 成本较低。
- 评估：`低`。

### 4.4 API 调用层迁移成本
- `apiGet/apiPost` 抽象简单，迁移到 Vue composables/axios wrapper 成本低。
- 评估：`低`。

### 4.5 Docker 迁移成本
- 需新增 Vue 前端构建镜像（Node 构建 + Nginx 或 Vite preview 运行），并调整 compose 服务定义。
- 后端、MySQL、Redis 无需改动。
- 评估：`低到中等`。

### 4.6 测试与联调成本（隐性）
- 登录态、权限、AI 对话、核心页面需重新回归。
- 文档与脚本要同步更新，避免前后端联调漂移。
- 评估：`中等`。

## 5. 迁移利弊

### 5.1 可能收益
- 若团队 Vue 经验更强，后续迭代效率可能提升。
- Vite 冷启动与热更新体验通常更快。
- 可借助成熟 Vue 后台组件生态快速补齐中后台页面。

### 5.2 主要风险
- 当前 MVP 仍在“先跑通主链路”阶段，框架切换会打断业务闭环推进。
- 现有 Next.js 代码、页面、组件、联调脚本会产生重写成本。
- 迁移阶段容易出现“功能冻结 + 回归负担”，影响可演示节奏。

## 6. 是否建议立即迁移
- 结论：**不建议立即迁移**。
- 理由：
  - 当前 Next.js 版本通过继续补齐页面与 UI 即可满足近期目标。
  - 当前关键目标是业务闭环与 Docker 可运行稳定性，不是框架替换。
  - 在现阶段迁移将引入非业务收益的交付风险。

## 7. 迁移范围建议（若未来决定迁移）
- 迁移对象：
  - 前端页面、路由、组件、状态、API 封装、前端 Dockerfile 与 compose 前端服务定义。
- 不迁移对象：
  - Spring Boot 后端、数据库脚本、Redis、鉴权协议、API 契约、AI Provider 抽象。

## 8. 分阶段迁移方案（建议）

### Phase 0：决策与对齐（P2）
- 明确迁移触发条件（如团队技能、招聘、性能、维护成本）。
- 建立“冻结窗口”：迁移期间只允许关键 bug 修复。

### Phase 1：Vue 技术样板（P1）
- 新建 `frontend-vue/`，不替换现 Next.js。
- 落地最小壳：登录页、布局壳、1-2 个业务页面、API 封装、Pinia、主题切换。
- 保持与当前后端同一套 `/api/v1` 契约与 JWT 机制。

### Phase 2：双前端并行验证（P1）
- Next.js 与 Vue 同时存在，使用不同端口/compose profile。
- 完成关键链路验证：登录、库存列表、AI 对话、客服会话。
- 对比开发效率、缺陷率、联调稳定性。

### Phase 3：增量替换（P1）
- 按业务域迁移（建议先 WMS 列表页，再 AI/客服，再报表）。
- 每迁移一个域即做回归，不做大爆炸式一次切换。

### Phase 4：切流与下线（P0）
- Vue 达到既定验收标准后切主入口。
- 归档 Next.js 前端，仅保留短期回滚窗口。

## 9. 优先级建议
- 近期（当前阶段）：`P0 = 继续增强现有 Next.js`，不迁移框架。
- 中期（当现有 MVP 稳定后）：`P1 = 做 Vue PoC（不替换主线）`。
- 长期（有明确收益证据后）：`P1/P0 = 分域迁移`。

## 10. 保留与重写清单

### 10.1 建议保留
- 现有 API 契约与鉴权方式（JWT + Bearer）。
- 环境变量命名与联调约定（`NEXT_PUBLIC_API_BASE_URL` 对应未来 `VITE_API_BASE_URL`）。
- 设计令牌与主题变量（`globals.css` 中变量体系）。
- Docker Compose 的后端/数据库/缓存编排方式。

### 10.2 建议重写
- 页面路由实现（App Router -> Vue Router）。
- React 组件与 hooks 逻辑（改为 Vue SFC + composables）。
- Zustand store（改为 Pinia）。
- Next 构建与运行配置（改为 Vite + Vue 前端 Dockerfile）。

## 11. 决策建议（最终）
- 在当前 NovaDepot 阶段，**优先保持 Next.js 主线并持续增强 UI/业务页面，不建议立即迁移到 Vue 3**。
- 若业务后续明确需要 Vue 生态优势，建议采用“`frontend-vue` 并行 PoC -> 分域替换”的稳妥路线，而非一次性切换。

## 12. 执行状态（2026-04-10）
- 项目决策已切换为“执行迁移”。
- 当前文档保留为迁移参考依据。
- 实施进度请以 `docs/19-vue-migration-phase1.md` 及后续阶段文档为准。
