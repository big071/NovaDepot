# NovaDepot 商用版本 Phase 2：主数据编辑与单据状态化变更规则（2026-04-13）

## 1. 阶段目标
把系统从“可新增”推进到“可维护、可纠错、可追踪”的商用后台标准。

## 2. 当前缺口

### 2.1 只有新增缺少编辑
1. 商品：前端无编辑入口。
2. 仓库：前端无编辑入口。
3. 库位：前端无编辑入口。
4. FAQ：前端无编辑入口。
5. 工单：缺责任人更新、备注更新闭环；状态更新前端入口缺失。

### 2.2 单据状态流转缺口
1. 入库单：无提交、撤回、作废、反审核，且无“仅草稿可编辑”规则。
2. 出库单：无提交、撤回、作废、反审核，且无“仅草稿可编辑”规则。

### 2.3 追加缺口
1. 强改密仅有前端提示，缺后端硬约束。
2. 缺管理员重置密码前端入口。
3. 缺审计中心前端页面。
4. 商品导入错误报告仅内存，重启后丢失。

## 3. 本轮执行范围
1. 主数据编辑能力：商品/仓库/库位/FAQ/工单（状态、责任人、备注）。
2. 单据状态化规则：入库单与出库单的编辑与状态动作最小可用闭环。
3. 详情与列表一致性：状态标签、按钮、详情状态一致刷新。
4. 追加项：
   - 强改密后端硬约束
   - 管理员重置密码前端入口
   - 审计中心前端页面
   - 商品导入错误报告持久化

## 4. 状态规则设计（最小可用）

### 4.1 入库单
1. 状态：`DRAFT -> SUBMITTED -> APPROVED -> POSTED`
2. 可编辑：仅 `DRAFT`
3. 撤回：`SUBMITTED -> DRAFT`
4. 反审核：`APPROVED -> SUBMITTED`
5. 作废：`DRAFT/SUBMITTED -> CANCELED`
6. `APPROVED/POSTED/CANCELED` 禁止直接编辑

### 4.2 出库单
1. 状态：`DRAFT -> SUBMITTED -> APPROVED -> SHIPPED`
2. 可编辑：仅 `DRAFT`
3. 撤回：`SUBMITTED -> DRAFT`
4. 反审核：`APPROVED -> SUBMITTED`
5. 作废：`DRAFT/SUBMITTED -> CANCELED`
6. `APPROVED/SHIPPED/CANCELED` 禁止直接编辑

### 4.3 强约束原则
1. 前端按钮状态与后端规则一致。
2. 后端拒绝非法流转与非法编辑。
3. 所有状态变更写审计。

## 5. 审计要求
1. 主数据编辑审计：`PRODUCT/WAREHOUSE/LOCATION/FAQ/TICKET`。
2. 单据动作审计：`SUBMIT/WITHDRAW/UNAPPROVE/CANCEL/APPROVE/POST/SHIP/UPDATE`。
3. 工单更新审计：状态、责任人、备注前后值。

## 6. 计划改动文件（Phase 2）
1. `docs/43-phase2-masterdata-edit-and-doc-status-rules-2026-04-13.md`
2. `backend/deploy/mysql/init/02-schema-wms.sql`
3. `backend/deploy/mysql/init/03-schema-erp-system-ai-cs.sql`
4. `backend/deploy/mysql/init/99-seed-mvp.sql`
5. `backend/src/main/java/com/novadepot/backend/security/SecurityConfig.java`
6. `backend/src/main/java/com/novadepot/backend/security/ForcePasswordChangeFilter.java`
7. `backend/src/main/java/com/novadepot/backend/repository/AuthQueryMapper.java`
8. `backend/src/main/java/com/novadepot/backend/modules/inboundorders/InboundOrdersController.java`
9. `backend/src/main/java/com/novadepot/backend/modules/inboundorders/InboundOrdersService.java`
10. `backend/src/main/java/com/novadepot/backend/modules/outboundorders/OutboundOrdersController.java`
11. `backend/src/main/java/com/novadepot/backend/modules/outboundorders/OutboundOrdersService.java`
12. `backend/src/main/java/com/novadepot/backend/modules/customerservice/CustomerServiceController.java`
13. `backend/src/main/java/com/novadepot/backend/modules/customerservice/CustomerServiceService.java`
14. `backend/src/main/java/com/novadepot/backend/model/entity/CustomerServiceTicketEntity.java`
15. `backend/src/main/java/com/novadepot/backend/modules/products/ProductsService.java`
16. `backend/src/main/java/com/novadepot/backend/modules/warehouses/WarehousesService.java`
17. `backend/src/main/java/com/novadepot/backend/modules/locations/LocationsService.java`
18. `backend/src/main/java/com/novadepot/backend/model/entity/ImportErrorReportEntity.java`
19. `backend/src/main/java/com/novadepot/backend/repository/ImportErrorReportMapper.java`
20. `frontend-vue/src/router/index.ts`
21. `frontend-vue/src/components/layout/SidebarNav.vue`
22. `frontend-vue/src/services/wms.ts`
23. `frontend-vue/src/services/customerService.ts`
24. `frontend-vue/src/services/auth.ts`
25. `frontend-vue/src/pages/wms/ProductsPage.vue`
26. `frontend-vue/src/pages/wms/WarehousesPage.vue`
27. `frontend-vue/src/pages/wms/LocationsPage.vue`
28. `frontend-vue/src/pages/wms/InboundPage.vue`
29. `frontend-vue/src/pages/wms/OutboundPage.vue`
30. `frontend-vue/src/pages/cs/CustomerServicePage.vue`
31. `frontend-vue/src/pages/system/UsersPage.vue`
32. `frontend-vue/src/pages/system/AuditCenterPage.vue`

## 7. 验收标准
1. 主数据编辑入口可见、可提交、失败提示明确。
2. 编辑后列表与详情同步刷新。
3. 单据状态动作与按钮权限一致，非法状态无法操作。
4. 后端严格校验状态流转。
5. 所有编辑与状态动作有审计记录。
6. 强改密用户除改密/登出外无法访问业务接口。
7. 管理员在前端可发起重置密码。
8. 审计中心页面可筛选并展示 before/after diff。
9. 商品导入错误报告重启后仍可按 reportId 查询。

## 8. 完成记录（2026-04-13）
### 8.1 已补齐编辑能力
1. 商品：前端新增“编辑”入口，编辑前按 ID 加载详情，保存后刷新列表与详情。
2. 仓库：前端新增“编辑”入口，编辑前按 ID 加载详情，保存后刷新列表与详情。
3. 库位：前端新增“编辑”入口，编辑前按 ID 加载详情，保存后刷新列表与详情。
4. FAQ：前端新增编辑弹窗，保存后刷新 FAQ 列表。
5. 工单：前端补齐状态更新、责任人更新、备注更新入口，保存后刷新工单列表。

### 8.2 单据状态规则（最小可用）
1. 入库单：
   - 草稿可编辑；
   - 提交/撤回/审核/反审核/作废/过账动作前端按钮严格按状态启用；
   - 动作后刷新列表与明细状态。
2. 出库单：
   - 草稿可编辑；
   - 提交/撤回/审核/反审核/作废/发运动作前端按钮严格按状态启用；
   - 动作后刷新列表与明细状态。

### 8.3 追加能力完成项
1. 首次登录强改密：后端 `ForcePasswordChangeFilter` 硬约束生效（仅允许改密/登出/登录/me）。
2. 管理员重置密码：新增前端“用户管理”页面入口与重置弹窗。
3. 审计中心：新增前端页面，支持筛选、分页、详情与 diff 展示。
4. 商品导入错误报告：后端改为持久化到 `import_error_reports`，支持重启后追溯。

### 8.4 仍未完成差距
1. 管理员用户列表当前仍是简化实现（后端 `UsersService` 为样例数据）。
2. 审计筛选暂未补 `bizNo/dateFrom/dateTo` 前端输入控件（后端接口已支持）。

### 8.5 验证记录
1. 前端执行：`npm run typecheck`，通过。
2. 后端本机未提供 `mvn/mvnw`，本轮未完成本地 Java 编译校验。
