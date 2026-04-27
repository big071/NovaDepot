# NovaDepot RC 收尾：UI 补完 + 性能优化 + 发布前验收（2026-04-12）

## 1. 当前未完成项清单
1. UI 基线未全覆盖：
   - 仓库管理页与库位管理页尚未完全对齐产品级 UI 基线。
   - 登录页视觉层次与状态反馈表现可继续收口。
   - 系统设置类页面在当前 Vue 前端尚未实现独立页面（本轮仅文档声明，不扩展新模块）。
2. 前端性能项未收口：
   - 路由仍使用静态导入，未懒加载。
   - 未定义明确页面分包策略，主包体积优化不足。
   - 图表组件可进一步按需/延迟加载。
   - 列表请求缺少统一缓存策略。
   - 仓库/库位列表缺少分页与筛选参数 URL 化。
3. RC 验收材料未形成统一发布文档：
   - 角色验收、主链路验收、Docker 验收、演示路径、回滚方案仍分散在历史文档中。

## 2. 本轮执行范围
1. UI 收口（仅既有页面）：
   - 仓库管理页
   - 库位管理页
   - 登录页
   - 系统设置页面：当前不存在，记录为 `N/A`（不新增模块）
2. 性能优化：
   - 路由懒加载
   - 页面分包与主包优化
   - ECharts 按需加载
   - 列表查询缓存优化
   - 分页与筛选参数优化（仓库/库位页）
3. 发布前验收材料补齐：
   - 角色验收清单
   - 主链路验收清单
   - Docker 验收清单
   - 演示路径验收清单
   - 回滚方案与注意事项

## 3. 本轮修改文件清单（计划）
1. 文档：
   - `docs/39-rc-closure-ui-performance-and-release-checklist-2026-04-12.md`
2. 前端：
   - `frontend-vue/src/router/index.ts`
   - `frontend-vue/vite.config.ts`
   - `frontend-vue/src/pages/DashboardPage.vue`
   - `frontend-vue/src/pages/LoginPage.vue`
   - `frontend-vue/src/pages/wms/WarehousesPage.vue`
   - `frontend-vue/src/pages/wms/LocationsPage.vue`
   - `frontend-vue/src/services/wms.ts`

## 4. 本轮验收标准
1. UI 一致性：
   - 仓库/库位/登录页对齐产品级 UI 基线（Hero、工具栏、表格壳层、状态条、空态容器）。
2. 功能稳定性：
   - 不改变现有接口契约，不破坏主链路。
3. 性能优化可观测：
   - 路由懒加载生效。
   - 构建结果可看到分包策略生效。
   - 图表组件仅在仪表盘加载。
   - 列表页重复进入具备缓存命中能力。
   - 仓库/库位页分页与筛选参数支持 URL 回放。
4. 兼容性：
   - 兼容 Docker 本地运行。

## 5. 角色验收清单（RC）
1. `admin`
   - 可访问全部主链路页面并执行核心操作。
2. `warehouse_manager`
   - 可进行仓库/库位/库存/入出库操作与状态推进。
3. `operator`
   - 可执行日常作业操作，受限于审批类权限。
4. `cs_agent`
   - 可处理客服会话、消息、工单与 FAQ。
5. `viewer`
   - 可查看数据，受限于变更类操作；403 反馈明确。

## 6. 主链路验收清单（RC）
1. 登录：成功/失败/登录失效重定向。
2. 仪表盘：指标、趋势、卡片跳转。
3. 商品：列表、创建、详情、筛选清除。
4. 仓库：列表、创建、详情、分页与筛选。
5. 库位：列表、创建、详情、分页与筛选。
6. 库存：列表、预警、流水、筛选清除。
7. 入库：创建、审核、过账、状态同步。
8. 出库：创建、审核、发运、库存不足提示。
9. AI 助手：会话、发消息、历史回放。
10. 客服工作台：会话、消息、建单、工单回查。
11. 工单：创建后可查。
12. FAQ：MySQL 数据源查询可用。

## 7. Docker 部署验收清单（RC）
1. `docker compose up -d` 可启动 `mysql/redis/backend/frontend-vue`。
2. 前端可访问并联调后端 API。
3. 初始化脚本可完成 schema + seed。
4. 停止与重启后服务可恢复。

## 8. 演示路径验收清单（RC）
1. `admin` 登录 -> 仪表盘 -> 商品/库存 -> 入库 -> 出库 -> AI -> 客服 -> FAQ。
2. `viewer` 登录 -> 浏览主链路 -> 触发受限动作 -> 页面内 403 提示。
3. 客服建单 -> 工单回查 -> FAQ 检索。

## 9. 回滚方案与注意事项
1. 前端回滚优先级：
   - `router/index.ts`（路由懒加载）
   - `vite.config.ts`（分包策略）
   - 页面文件（UI/分页筛选改造）
   - `services/wms.ts`（缓存逻辑）
2. 若出现异常：
   - 先回滚性能策略（懒加载/分包/缓存），保留功能修复。
   - 再回滚页面视觉层，确保功能可用优先。
3. 注意事项：
   - 缓存仅用于列表读操作，写操作后需失效处理。
   - URL 参数同步需保证刷新可恢复状态且不触发死循环更新。

## 10. 本轮实际完成记录（待填充）
1. 已完成项：
   - 仓库管理页已升级为产品级 UI 基线（Hero、工具栏、表格壳层、状态条、空态容器）。
   - 库位管理页已升级为产品级 UI 基线，并补齐筛选与分页 URL 参数回放。
   - 登录页已升级为产品级视觉基线，保留登录失效提示与表单可用性约束。
   - 系统设置类页面现状已确认：当前前端无 `system/*` 实装页面，本轮按 `N/A` 记录，未扩展新模块。
   - 路由已改为懒加载，页面级 chunk 已拆分。
   - Vite 已添加 manual chunk 策略（core / naive / echarts / misc）。
   - Dashboard 图表已改为异步加载 `vue-echarts + echarts`（仅仪表盘路由触发）。
   - WMS 列表查询已接入轻量缓存（TTL + 写后失效），减少重复请求。
   - 仓库/库位页已支持分页与筛选参数 URL 化（`q/page/pageSize/warehouseId`）。
2. 验证结果：
   - `npm run typecheck`：通过。
   - `npm run build`：通过。
   - 构建体积对比（前端）：
     - 优化前（历史记录）：主 `index-*.js` 约 `1239.07 kB`。
     - 优化后：入口 `index-*.js` 约 `6.79 kB`，页面 chunk 独立拆分；`echarts` 独立至 `vendor-echarts`（约 `1018.52 kB`）。
     - 结论：主包显著下降，重资源转为按路由按需加载。

## 11. 本轮实际修改文件清单
1. 文档：
   - `docs/39-rc-closure-ui-performance-and-release-checklist-2026-04-12.md`
2. 前端：
   - `frontend-vue/src/router/index.ts`
   - `frontend-vue/vite.config.ts`
   - `frontend-vue/src/services/wms.ts`
   - `frontend-vue/src/pages/DashboardPage.vue`
   - `frontend-vue/src/pages/LoginPage.vue`
   - `frontend-vue/src/pages/wms/WarehousesPage.vue`
   - `frontend-vue/src/pages/wms/LocationsPage.vue`

## 12. RC 发布前验证步骤（执行版）
1. 启动服务：
   - `docker compose up -d mysql redis backend frontend-vue`
2. 角色验收：
   - 分别使用 `admin / warehouse_manager / operator / cs_agent / viewer` 登录执行角色清单。
3. 主链路验收：
   - 按第 6 节 12 项链路逐项验证（含失败路径提示）。
4. 自动化与门禁：
   - 本地：`npm run typecheck && npm run build`
   - PR：确认 `Backend Smoke` 与 `Frontend E2E` 通过。
5. 演示回放：
   - 按第 8 节演示路径跑通，并截图留档。

## 13. RC 判定
1. 当前状态：满足“RC 候选版”标准（主链路、演示数据、自动化测试、CI 门禁、核心 UI、性能收口已完成）。
2. 保留注意：
   - `vendor-echarts` 仍为大体积依赖，但已脱离主包并路由按需加载，可在正式发布前按图表场景继续细分优化。
