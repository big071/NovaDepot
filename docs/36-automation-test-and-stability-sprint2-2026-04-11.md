# NovaDepot 自动化测试与稳定化 Sprint 2（2026-04-11）

## 1. 目标
- 在不新增业务模块的前提下，完成稳定化收口与自动化测试补齐。
- 聚焦主链路的可回归性：权限反馈统一、关键动作状态一致、后端冒烟、前端 E2E。

## 2. 当前测试缺口
1. 后端缺少 `src/test` 冒烟回归，主链路接口未形成自动化门禁。
2. 前端缺少 E2E 基础设施（Playwright/Cypress 均未接入）。
3. 入库/出库状态流转、客服建单回查、AI 会话历史无自动化保障。
4. 401/403 权限场景未自动化验证，前端页面反馈不统一。

## 3. 当前稳定性问题
1. 403 场景多为通用错误文案，页面内未统一显式“无权限”。
2. 长列表页面在操作成功/失败后的可见状态反馈粒度不一致。
3. 部分页面动作禁用、操作中、刷新后一致性仍依赖分散实现。

## 4. 本轮执行范围
1. 稳定化：
   - 统一 403 页面内反馈（明确“无权限”语义）。
   - 统一关键页面状态反馈：`loading / operation loading / success / error / disabled`。
2. 后端自动化测试（冒烟）：
   - 登录
   - 仪表盘
   - 商品列表
   - 仓库列表
   - 库位列表
   - 库存列表
   - 入库创建/审核/过账
   - 出库创建/审核/发运
   - AI 会话列表/发消息/历史回放
   - 客服会话/建工单/工单回查/FAQ
3. 前端 E2E：
   - 登录
   - 入库流转
   - 出库流转
   - 客服建单并回查
   - AI 会话发送与历史展示

## 5. 计划修改文件
1. 文档：
   - `docs/36-automation-test-and-stability-sprint2-2026-04-11.md`
2. 前端（预计）：
   - `frontend-vue/src/services/api.ts`
   - `frontend-vue/src/layouts/WorkspaceLayout.vue`
   - `frontend-vue/src/pages/wms/ProductsPage.vue`
   - `frontend-vue/src/pages/wms/WarehousesPage.vue`
   - `frontend-vue/src/pages/wms/LocationsPage.vue`
   - `frontend-vue/src/pages/wms/InventoryPage.vue`
   - `frontend-vue/src/pages/wms/InboundPage.vue`
   - `frontend-vue/src/pages/wms/OutboundPage.vue`
   - `frontend-vue/src/pages/ai/AiAssistantPage.vue`
   - `frontend-vue/src/pages/cs/CustomerServicePage.vue`
   - `frontend-vue/package.json`
   - `frontend-vue/playwright.config.ts`
   - `frontend-vue/tests/e2e/*`
3. 后端（预计）：
   - `backend/src/test/java/com/novadepot/backend/smoke/*`
   - `backend/src/test/resources/application-test.yml`
   - `backend/pom.xml`（如需补测试插件）

## 6. 验收标准
1. 关键页面 403 时，页面内出现统一无权限提示，不再仅依赖 toast。
2. 主链路关键页面具备并可观测：loading、操作中、成功反馈、失败反馈、禁用态。
3. 后端冒烟测试可本地执行并覆盖指定接口链路。
4. 前端 E2E 可本地执行并覆盖指定 5 条流程。
5. Docker 本地运行兼容，不改变现有接口契约。

## 7. 运行方式（本轮目标）
1. 后端测试：`mvn test`
2. 前端 E2E：
   - `npm run test:e2e:install`
   - `npm run test:e2e`
3. Docker 启动（依赖服务）：`docker compose up -d`

## 8. 约束说明
- 不新增模块、不扩展多租户/商业化能力。
- 不做无关 UI 大改，仅做稳定性相关状态反馈统一。
- 保持 Vue 3 + Spring Boot 3 + MySQL 8 + Redis + Docker Compose 路线。

## 9. 本轮实际完成
1. 403 反馈统一：
   - 前端 API 层新增 `ApiRequestError`，识别 HTTP 403 或业务 `FORBIDDEN` 并派发统一事件。
   - 工作台布局新增页面内权限提示条（非仅 toast）。
   - 后端修复 `BizException(FORBIDDEN)` 状态码映射，返回 403（此前为 400）。
2. 长列表状态一致性补齐：
   - 入库/出库/商品/仓库/库位/AI/客服页统一将关键动作失败写入页面 `errorText`。
   - 入库/出库动作期间统一禁用明细和流转按钮，避免并发点击造成状态错乱。
3. 后端自动化测试：
   - 新增 `MainlineSmokeTest`，覆盖登录、仪表盘、主数据、库存、入库、出库、AI、客服、FAQ、权限拒绝断言。
4. 前端 E2E：
   - 接入 Playwright，新增 5 条端到端回归用例并全部通过。

## 10. 实际修改文件
1. 文档：
   - `docs/36-automation-test-and-stability-sprint2-2026-04-11.md`
2. 后端：
   - `backend/src/main/java/com/novadepot/backend/common/exception/GlobalExceptionHandler.java`
   - `backend/src/test/java/com/novadepot/backend/smoke/MainlineSmokeTest.java`
   - `backend/src/test/resources/application-test.yml`
3. 前端：
   - `frontend-vue/src/services/api.ts`
   - `frontend-vue/src/layouts/WorkspaceLayout.vue`
   - `frontend-vue/src/pages/wms/ProductsPage.vue`
   - `frontend-vue/src/pages/wms/WarehousesPage.vue`
   - `frontend-vue/src/pages/wms/LocationsPage.vue`
   - `frontend-vue/src/pages/wms/InboundPage.vue`
   - `frontend-vue/src/pages/wms/OutboundPage.vue`
   - `frontend-vue/src/pages/ai/AiAssistantPage.vue`
   - `frontend-vue/src/pages/cs/CustomerServicePage.vue`
   - `frontend-vue/package.json`
   - `frontend-vue/package-lock.json`
   - `frontend-vue/playwright.config.ts`
   - `frontend-vue/tests/e2e/helpers.ts`
   - `frontend-vue/tests/e2e/login.spec.ts`
   - `frontend-vue/tests/e2e/wms-flow.spec.ts`
   - `frontend-vue/tests/e2e/customer-service.spec.ts`
   - `frontend-vue/tests/e2e/ai-assistant.spec.ts`

## 11. 测试结果
1. 后端冒烟：
   - 运行：`docker run ... maven:3.9-eclipse-temurin-17 sh -lc "mvn test -Dspring.profiles.active=test -q"`
   - 结果：通过（`MainlineSmokeTest` 8/8）。
2. 前端类型检查与构建：
   - `npm run typecheck` 通过。
   - `npm run build` 通过。
3. 前端 E2E：
   - `npm run test:e2e` 通过（5/5）。

## 12. 本地复现步骤
1. 启动依赖：`docker compose up -d mysql redis backend`
2. 后端冒烟（建议 Docker Maven）：
   - `docker run --rm -v "<repo>/backend:/workspace" -w /workspace --network novadepot_novadepot-net -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/novadepot -e SPRING_DATASOURCE_USERNAME=root -e SPRING_DATASOURCE_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -e SPRING_DATA_REDIS_PORT=6379 maven:3.9-eclipse-temurin-17 sh -lc "mvn test -Dspring.profiles.active=test -q"`
3. 前端 E2E：
   - `cd frontend-vue`
   - `npm install`
   - `npm run test:e2e:install`
   - `npm run test:e2e`

## 13. 当前已完成能力（Sprint 2）
1. 主链路自动化测试基线已建立（后端冒烟 + 前端 E2E）。
2. 权限拒绝场景已能在页面内统一识别和提示。
3. 关键长列表页面动作状态反馈一致性已提升。

## 14. 当前未完成/后续建议
1. E2E 仍偏“主路径”，可继续补失败分支断言（如库存不足、无权限角色跳转）。
2. 可增加 CI 执行脚本，将后端冒烟与前端 E2E纳入 PR 门禁。
3. 后续可在不改接口前提下继续统一空态文案与组件样式（P2/P3）。
