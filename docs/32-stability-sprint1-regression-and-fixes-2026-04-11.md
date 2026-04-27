# NovaDepot 稳定化 Sprint 1：主链路回归与稳定性修复（2026-04-11）

## 1. 回归范围
- 登录
- 仪表盘
- 商品
- 仓库
- 库位
- 库存
- 入库
- 出库
- AI 助手
- 客服工作台
- 工单
- FAQ

## 2. 回归结果（本轮开始前）
- API 主链路回归通过：`FAILED_COUNT=0`
- 关键接口验证通过：
  - `POST /api/v1/auth/login`
  - `GET /api/v1/reports/dashboard`
  - `GET /api/v1/products`
  - `GET /api/v1/warehouses`
  - `GET /api/v1/locations`
  - `GET /api/v1/inventory`
  - `GET /api/v1/inbound-orders`
  - `GET /api/v1/outbound-orders`
  - `GET /api/v1/ai/conversations`
  - `POST /api/v1/ai/chat`
  - `GET /api/v1/ai/conversations/by-no/{conversationNo}/messages`
  - `GET /api/v1/customer-service/sessions`
  - `POST /api/v1/customer-service/tickets`
  - `GET /api/v1/customer-service/tickets`
  - `GET /api/v1/customer-service/faq`

## 3. 当前问题清单（按优先级）

### P0（阻塞使用）
- 本轮未发现新的 P0。

### P1（高频影响操作）
1. 多个关键页面缺少可见 `error/empty` 状态区，仅依赖 toast，失败后页面状态可读性不足。
2. 多个关键动作缺少充分 `disabled` 控制，前置条件不满足时仍可点击后报错。
3. 成功反馈主要是瞬时 toast，缺少页面内可见的“最近成功刷新/处理”状态。
4. 路由筛选恢复入口不完全统一，存在状态一致性风险。

### P2（体验问题）
1. 个别页面状态呈现风格不一致（空态、错误重试入口样式不统一）。

### P3（视觉与细节）
1. 空态/提示文案细节有待统一。

## 4. 本轮执行范围
- 仅修复 P0/P1；不新增业务模块，不做无关重构。
- 重点做页面状态稳定化：`loading/empty/error/success feedback/disabled state`。

## 5. 本轮修改文件（计划）
- `frontend-vue/src/pages/LoginPage.vue`
- `frontend-vue/src/pages/DashboardPage.vue`
- `frontend-vue/src/pages/wms/ProductsPage.vue`
- `frontend-vue/src/pages/wms/WarehousesPage.vue`
- `frontend-vue/src/pages/wms/LocationsPage.vue`
- `frontend-vue/src/pages/wms/InventoryPage.vue`
- `frontend-vue/src/pages/wms/InboundPage.vue`
- `frontend-vue/src/pages/wms/OutboundPage.vue`
- `frontend-vue/src/pages/ai/AiAssistantPage.vue`
- `frontend-vue/src/pages/cs/CustomerServicePage.vue`

## 6. 验收标准
1. 关键页面具备 loading、empty、error、success feedback、disabled state。
2. 前置条件不足的关键按钮明确禁用或给出明确提示。
3. 主链路接口回归通过，无新增 mock 分支。
4. Docker 本地运行兼容，无回归。

## 7. 本轮修复记录
1. 登录页：
   - 登录按钮增加 `disabled` 规则（输入不完整或正在提交时禁用）。
2. 仪表盘：
   - 增加可见错误态（含重试按钮）。
   - 增加可见成功态（最近刷新时间）。
   - 增加空态（趋势/风险无数据时显示 Empty）。
3. 商品页：
   - 增加可见错误态、成功态、空态。
   - 新增“保存”按钮禁用规则（必填项缺失时不可提交）。
4. 仓库页：
   - 增加刷新按钮与可见错误态、成功态、空态。
   - 新增“保存”按钮禁用规则（必填项缺失时不可提交）。
5. 库位页：
   - 增加刷新按钮与可见错误态、成功态、空态。
   - 新增“保存”按钮禁用规则。
   - “新增库位”在无仓库基础数据时禁用。
6. 库存页：
   - 增加可见错误态、成功态。
   - 库存列表、低库存、流水三块都补齐空态。
7. 入库页：
   - 增加可见错误态、成功态、列表空态、明细空态。
   - 新增表单 `canCreate` 禁用规则（主数据加载中/必填缺失/数量不合法时禁用）。
   - 增加“来自仪表盘筛选”清除入口，保证状态可逆。
8. 出库页：
   - 增加可见错误态、成功态、列表空态、明细空态。
   - 新增表单 `canCreate` 禁用规则。
   - 增加“来自仪表盘筛选”清除入口。
9. AI 助手页：
   - 会话列表空态、发送按钮禁用规则（空输入/发送中/消息加载中）。
   - 增加可见成功态（发送成功时间）。
   - 会话与消息加载失败统一写入页面错误态。
10. 客服工作台：
   - 会话、消息、FAQ 补齐空态。
   - 消息加载态可见化。
   - 发送、转人工、建工单增加禁用条件。
   - 增加可见成功态（消息发送/转人工/建单）。
11. 前端 API 客户端：
   - 增加对 `ApiResponse.code != \"0\"` 的统一失败判定，避免 200 但业务失败时误判成功。

## 8. 当前已完成能力
- RBAC 登录（users/roles/permissions）已生效。
- 客服 sessions/messages/tickets/faq 已接入 MySQL 数据源。
- 入库/出库已支持详情列表（页面内展开）与审核/过账/发运链路。
- 仪表盘 KPI 卡片已支持路由跳转。

## 9. 当前未完成能力
- 暂无新增业务能力，本轮仅稳定化。
- 仍需后续补齐更细粒度页面内 skeleton 与分页级异常恢复（属于 P2/P3）。

## 10. 下一轮建议（预留）
1. P1：补齐“权限拒绝（403）”页面内专属提示（当前主要依赖 toast + message）。
2. P2：统一各页空态文案与插图风格，增强一致性。
3. P2：优化大体积前端打包（当前构建提示主包超 500 kB）。
4. P3：补充关键交互的 E2E 回归脚本（登录、出入库、AI、客服工单）。

## 11. 当前 Bug 清单（回归后）
- P0：无。
- P1：无新增阻断性问题。
- P2：构建输出主包体积偏大（`index-*.js` 超过 500 kB，需后续做分包优化）。
- P3：个别页面文案与状态提示样式仍可再统一。

## 12. 验证记录
1. 前端构建：
   - 执行 `npm install`（前端依赖缺失修复）。
   - 执行 `npm run build` 成功（`vue-tsc --noEmit && vite build` 通过）。
2. 主链路接口回归：
   - 登录、仪表盘、商品、仓库、库位、库存、入库、出库、AI、客服、工单、FAQ 均返回成功。

## 13. P1/P2/P3 清单状态（下一轮承接）
- P1（稳定性）：
  - 已完成：关键页面 `loading/empty/error/success feedback/disabled` 基础补齐。
  - 待继续：403 权限拒绝场景的页面内专属提示与引导。
- P2（体验）：
  - 已识别：前端主包体积告警（>500kB），空态文案和状态提示样式可继续统一。
  - 待继续：按路由/模块做分包与懒加载优化。
- P3（细节）：
  - 已识别：个别页面文案和提示细节仍可优化。
  - 待继续：补关键链路 E2E 回归脚本，减少回归遗漏风险。
