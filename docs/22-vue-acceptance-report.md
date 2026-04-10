# NovaDepot Vue 版本验收报告（阶段 1-2）

## 1. 验收范围
本次仅验收 Vue 前端（`frontend-vue`）以及与现有后端联调能力，不删除旧 Next.js 前端。

覆盖项：
1. 中文显示与字符集
2. 登录链路
3. 仪表盘
4. WMS 核心页面（商品/仓库/库位/库存/入库/出库）
5. AI 助手（免费方案）
6. 客服工作台
7. 按钮反馈完整性
8. Docker 可访问性
9. 前后端联调
10. 环境与部署配置一致性

## 2. 验收发现

### 2.1 通过项（当前）
- Vue 页面可访问（`http://localhost:3100`）。
- 响应头包含 `charset=utf-8`，中文显示链路完整。
- 登录、商品、库存、AI、客服关键接口链路联调通过。
- `VITE_API_BASE_URL`、`nginx.conf`、`frontend-vue/Dockerfile` 与 compose 配置一致。

### 2.2 发现问题（需修复）
- 全量执行 `docker compose up --build` 时，旧 Next.js 构建会误包含 `frontend-vue` 源码并触发类型检查失败。
- 该问题会影响“整仓一键构建”体验，但不影响已运行的 Vue 服务。

## 3. 修复策略（小步）
1. 在根项目 `tsconfig.json` 中排除 `frontend-vue`。
2. 在根 `.dockerignore` 中排除 `frontend-vue`，避免旧前端镜像构建复制该目录。
3. 复测 `docker compose up --build`。

## 4. 结论口径
- 修复完成并复测通过后，可进入“Vue 正式切换准备阶段”。
- 旧 Next.js 前端暂不删除，仅保留为回退链路，等待用户确认后执行清理计划。

## 5. 修复结果与复测

### 5.1 已执行修复
- `tsconfig.json`：增加 `exclude: ["frontend-vue"]`，避免旧 Next.js 类型检查误扫 Vue 源码。
- `.dockerignore`：增加 `frontend-vue`，避免旧 Next.js 镜像构建复制 Vue 目录。

### 5.2 复测结果
- `docker compose up --build -d`：通过。
- `docker compose ps`：`mysql` / `redis` / `backend` / `frontend-vue` / `frontend` 全部运行中。
- `curl -I http://localhost:3100`：`Content-Type: text/html; charset=utf-8`。
- 核心接口链路均返回 `code=0`：
  - 登录
  - 仪表盘
  - 商品/仓库/库位/库存/入库/出库列表
  - AI chat/conversations（免费方案）
  - 客服 sessions/messages/transfer/tickets/faq

## 6. Vue 切换准备结论
- 当前 Vue 版本已达到“可正式切换准备”状态。
- 旧 Next.js 前端可进入“待清理”阶段，但应在确认回退窗口后执行删除。
