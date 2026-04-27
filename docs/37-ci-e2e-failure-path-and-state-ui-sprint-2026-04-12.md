# NovaDepot CI 门禁 + 失败路径 E2E + 状态视觉统一 Sprint（2026-04-12）

## 1. 目标
- 将后端冒烟与前端 E2E 纳入 CI，作为 PR 门禁。
- 补齐失败路径 E2E：库存不足、权限拒绝、网络异常重试。
- 在不改业务契约前提下，统一空态文案与状态组件视觉（P2/P3）。

## 2. 当前问题清单
1. 当前没有 GitHub Actions 工作流，PR 缺少自动化门禁。
2. E2E 仅覆盖主路径，对失败分支保障不足。
3. 多页面空态与状态提示样式虽可用，但视觉统一度不足。

## 3. 本轮执行范围
1. CI：
   - 新增 `.github/workflows/ci.yml`。
   - PR 触发后执行：
     - 后端冒烟测试（Spring Boot + MySQL + Redis）
     - 前端 E2E（Playwright）
2. 前端 E2E：
   - 新增失败路径用例：
     - 库存不足出库失败提示
     - 低权限用户触发 403 页面内提示
     - 列表网络异常后点击重试恢复
3. P2/P3 视觉统一：
   - 统一状态提示条（error/success/info）样式类
   - 统一空态容器样式与文案风格

## 4. 修改文件清单
1. 文档：
   - `docs/37-ci-e2e-failure-path-and-state-ui-sprint-2026-04-12.md`
2. CI：
   - `.github/workflows/ci.yml`
3. 前端测试：
   - `frontend-vue/tests/e2e/helpers.ts`
   - `frontend-vue/tests/e2e/wms-flow.spec.ts`
   - `frontend-vue/tests/e2e/failure-paths.spec.ts`
4. 前端视觉统一：
   - `frontend-vue/src/styles/main.css`
   - `frontend-vue/src/pages/DashboardPage.vue`
   - `frontend-vue/src/pages/wms/ProductsPage.vue`
   - `frontend-vue/src/pages/wms/InventoryPage.vue`
   - `frontend-vue/src/pages/wms/InboundPage.vue`
   - `frontend-vue/src/pages/wms/OutboundPage.vue`
   - `frontend-vue/src/pages/ai/AiAssistantPage.vue`
   - `frontend-vue/src/pages/cs/CustomerServicePage.vue`

## 5. 验收标准
1. PR 自动触发 CI，且必须通过 `backend-smoke` 与 `frontend-e2e`。
2. 失败路径 E2E 可本地执行通过。
3. 关键页面空态与状态提示视觉统一，不影响现有功能。
4. 兼容 Docker 本地运行。

## 6. 本地运行方式
1. 启动依赖：`docker compose up -d mysql redis backend`
2. 后端冒烟：
   - 有 Maven：`mvn -f backend/pom.xml test -Dspring.profiles.active=test`
   - 无 Maven：使用 maven 容器（沿用 docs/36 命令）
3. 前端 E2E：
   - `cd frontend-vue`
   - `npm install`
   - `npm run test:e2e:install`
   - `npm run test:e2e`

## 7. 风险与回滚
1. CI 依赖 Playwright 浏览器下载，首次执行耗时较长。
2. E2E 使用示例数据，若种子数据结构变化需同步维护用例。
3. 所有改动均为增量，可按文件级回滚。

## 8. 本轮实际完成
1. CI 门禁：
   - 新增 `.github/workflows/ci.yml`。
   - PR 将执行两条门禁：
     - `backend-smoke`
     - `frontend-e2e`
2. 失败路径 E2E：
   - 新增 `frontend-vue/tests/e2e/failure-paths.spec.ts`，覆盖：
     - 库存不足导致发运失败
     - 权限不足触发页面内 403 提示
     - 列表网络异常后点击重试恢复
   - `helpers.ts` 增加通用登录函数 `loginAs`。
3. P2/P3 视觉统一：
   - 新增状态/空态样式类：`nd-state-alert`、`nd-empty-shell`。
   - 已在仪表盘、商品、库存、入库、出库、AI、客服、工作台布局统一使用。

## 9. 本轮实际修改文件
1. `docs/37-ci-e2e-failure-path-and-state-ui-sprint-2026-04-12.md`
2. `.github/workflows/ci.yml`
3. `frontend-vue/tests/e2e/helpers.ts`
4. `frontend-vue/tests/e2e/failure-paths.spec.ts`
5. `frontend-vue/src/styles/main.css`
6. `frontend-vue/src/layouts/WorkspaceLayout.vue`
7. `frontend-vue/src/pages/DashboardPage.vue`
8. `frontend-vue/src/pages/wms/ProductsPage.vue`
9. `frontend-vue/src/pages/wms/InventoryPage.vue`
10. `frontend-vue/src/pages/wms/InboundPage.vue`
11. `frontend-vue/src/pages/wms/OutboundPage.vue`
12. `frontend-vue/src/pages/ai/AiAssistantPage.vue`
13. `frontend-vue/src/pages/cs/CustomerServicePage.vue`

## 10. 验证情况
1. 前端类型检查：`npm run typecheck` 通过。
2. 本地 E2E 回归：受当前环境 `Docker Desktop paused` 影响，后端依赖不可用，E2E 无法完成有效联调验证（已在日志中体现）。
3. CI 设计已将 MySQL/Redis 初始化、后端冒烟、前端 E2E 全链路纳入 PR 自动执行。

## 11. PR 门禁启用说明
1. 将以下检查设置为仓库 Branch Protection Required Checks：
   - `Backend Smoke`
   - `Frontend E2E`
2. 未通过任一检查时禁止合并 PR。
