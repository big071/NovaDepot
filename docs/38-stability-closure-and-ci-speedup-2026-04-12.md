# NovaDepot 稳定化收口与失败路径扩展 Sprint（2026-04-12）

## 1. 当前问题清单
1. 上轮未完成：
   - 本地 E2E 未完成有效联调回归（当时 Docker Desktop paused）。
   - PR Required checks 需明确落地路径与校验口径。
2. 失败路径仍缺：
   - 登录失效自动重定向登录页。
   - 后端 5xx 全局回退提示。
3. CI 运行时长仍可优化：
   - Playwright 浏览器缓存缺失。
   - `backend-smoke` 与 `frontend-e2e` 串行执行，整体耗时偏长。

## 2. 本轮执行范围
1. 完成上轮未完成项：
   - 在本地恢复依赖后完成一次有效 E2E 回归。
   - 文档补齐 PR Required checks 启用与验收方法。
2. 补两条失败路径：
   - 登录失效重定向。
   - 后端 5xx 全局回退。
3. CI 加速：
   - 增加 Playwright 缓存。
   - 调整作业并行策略（后端与前端 E2E并行门禁）。

## 3. 本轮修改文件清单
1. 文档：
   - `docs/38-stability-closure-and-ci-speedup-2026-04-12.md`
2. 前端实现：
   - `frontend-vue/src/services/api.ts`
   - `frontend-vue/src/layouts/WorkspaceLayout.vue`
   - `frontend-vue/src/pages/LoginPage.vue`
3. 前端 E2E：
   - `frontend-vue/tests/e2e/failure-paths.spec.ts`
   - `frontend-vue/tests/e2e/helpers.ts`
4. CI：
   - `.github/workflows/ci.yml`

## 4. 本轮验收标准
1. 无效/过期 token 时，页面会统一回到登录页并提示登录失效。
2. 任意关键请求出现 5xx 时，页面顶层出现统一“系统异常”回退提示。
3. 新增失败路径 E2E 用例可通过。
4. CI 具备 Maven 缓存 + Playwright 缓存，且后端/前端门禁并行执行。
5. 保持 Docker 本地可运行，不新增无关模块。

## 5. 实际修复结果
1. 失败路径补齐：
   - 已新增并通过 `登录失效重定向` E2E。
   - 已新增并通过 `后端 5xx 全局回退` E2E。
2. 全局状态反馈：
   - `api.ts` 统一分发 `unauthorized / forbidden / server-error` 事件。
   - `WorkspaceLayout` 顶层统一展示 401/403/5xx 页面内反馈。
   - 登录页支持 `reason=expired` 明确提示“登录状态已失效”。
3. CI 门禁与加速：
   - 已新增 `.github/workflows/ci.yml`，包含 `backend-smoke` + `frontend-e2e` 双门禁。
   - 已启用 Maven 缓存（`actions/setup-java cache=maven`）。
   - 已启用 Playwright 浏览器缓存（`~/.cache/ms-playwright`）。
   - 已移除 `frontend-e2e` 对 `backend-smoke` 的 `needs`，改为并行执行。

## 6. 本地验证记录
1. 前端校验：
   - `npm run typecheck`：通过
   - `npm run build`：通过
2. 前端失败路径 E2E：
   - `npm run test:e2e -- tests/e2e/failure-paths.spec.ts`：5/5 通过
3. 后端冒烟（Docker Maven 容器）：
   - 执行命令：`docker run --rm --network novadepot_novadepot-net -v "D:\新建文件夹\NovaDepot:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-17 sh -lc "mvn -f backend/pom.xml test -Dspring.profiles.active=test '-Dspring.datasource.url=jdbc:mysql://novadepot-mysql:3306/novadepot?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' -Dspring.datasource.username=root -Dspring.datasource.password=root -Dspring.data.redis.host=novadepot-redis -Dspring.data.redis.port=6379"`。
   - 执行结果：`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
