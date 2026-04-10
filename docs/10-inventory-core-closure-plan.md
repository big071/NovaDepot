# NovaDepot 库存核心闭环实施方案（第一批）

## A. 业务规则说明
1. 商品、仓库、库位是库存业务前置主数据。
2. 库存快照（`inventory`）只允许通过入库/出库/调拨/盘点流程变更。
3. 任意库存变更必须写入库存流水（`inventory_transactions`）。
4. 入库单完成后增加可用库存；出库单完成后减少可用库存。
5. 出库扣减前必须校验可用库存充足。
6. 单据状态流转必须单向可追溯，不允许越级跳转。
7. 低库存预警基于“可用库存 <= 安全阈值”判定（MVP 固定阈值，后续参数化）。

## B. 状态流转设计
- 入库单：`DRAFT -> SUBMITTED -> APPROVED -> COMPLETED -> CANCELED`
- 出库单：`DRAFT -> SUBMITTED -> APPROVED -> COMPLETED -> CANCELED`
- 库存流水业务类型：`INBOUND`、`OUTBOUND`、`MANUAL_ADJUST`（MVP）

## C. 后端实现顺序
1. 主数据写接口：商品、仓库、库位
2. 库存查询与流水查询接口
3. 入库单创建 + 审核 + 入账（库存增加 + 流水）
4. 出库单创建 + 审核 + 发运（库存减少 + 流水）
5. 低库存预警接口
6. 仪表盘统计接口（库存总数、今日入/出库单、预警数）
7. 审计日志埋点（关键动作）

## D. 前端实现顺序
1. 商品列表页（接入真实 API）
2. 库存列表页（接入真实 API）
3. 入库列表页（创建/审核/完成动作）
4. 出库列表页（创建/审核/完成动作）
5. 仪表盘页（统计卡片 + 趋势）
6. 低库存预警展示（库存页二级筛选）
7. 统一错误反馈与加载态
8. 环境变量切换 mock/real API

## E. 关键接口设计（库存闭环）
- `POST /api/v1/products`
- `POST /api/v1/warehouses`
- `POST /api/v1/locations`
- `GET /api/v1/inventory`
- `GET /api/v1/inventory/transactions`
- `GET /api/v1/inventory/alerts/low-stock`
- `POST /api/v1/inbound-orders`
- `POST /api/v1/inbound-orders/{id}/actions/approve`
- `POST /api/v1/inbound-orders/{id}/actions/post`
- `POST /api/v1/outbound-orders`
- `POST /api/v1/outbound-orders/{id}/actions/approve`
- `POST /api/v1/outbound-orders/{id}/actions/ship`
- `GET /api/v1/reports/dashboard`

## F. 关键数据校验规则
1. `product_id`、`warehouse_id`、`location_id` 必须存在且状态可用。
2. 单据明细 `qty > 0`。
3. 出库前校验 `inventory.available_qty >= ship_qty`。
4. 入库/出库动作携带 `requestId` 时要求幂等（MVP 先约定字段，后续加唯一约束校验）。
5. 单据状态校验：仅允许在指定状态执行动作。

## G. 异常场景处理
1. 库存不足：拒绝出库并返回业务错误码。
2. 单据状态非法：拒绝动作并返回状态错误。
3. 主数据不存在：返回参数错误并提示具体字段。
4. 库存并发更新冲突：返回重试提示（后续加版本号 CAS）。
5. 库存流水写入失败：事务回滚，保证库存与流水一致。

## H. 审计日志记录规则
以下动作必须落审计：
- 商品/仓库/库位创建与修改
- 入库单创建、审核、完成
- 出库单创建、审核、完成
- 库存调整（后续）

建议审计字段：
- `module`、`action`、`resource_type`、`resource_id`、`before_json`、`after_json`、`operator_id`、`occurred_at`

## 第一批最应该写的 10 个接口
1. `POST /api/v1/products`
2. `GET /api/v1/products`
3. `POST /api/v1/warehouses`
4. `POST /api/v1/locations`
5. `GET /api/v1/inventory`
6. `POST /api/v1/inbound-orders`
7. `POST /api/v1/inbound-orders/{id}/actions/approve`
8. `POST /api/v1/inbound-orders/{id}/actions/post`
9. `POST /api/v1/outbound-orders`
10. `POST /api/v1/outbound-orders/{id}/actions/ship`

## 第一批最应该写的 8 个前端页面
1. `/login`
2. `/dashboard`
3. `/wms/products`
4. `/wms/inventory`
5. `/wms/inbound`
6. `/wms/outbound`
7. `/wms/warehouses`（新增）
8. `/wms/locations`（新增）

## 第一批最小可运行代码落地方案
- 后端：
  - 完成上述 10 个接口 + 低库存预警 + 仪表盘统计
  - MySQL 初始化脚本可直接被 Docker 自动执行
  - Redis 保留连接与缓存预留（MVP 可先不强依赖）
- 前端：
  - 保持现有骨架，增加 API 客户端
  - 先将商品/库存/入库/出库/仪表盘改为真实 API 驱动
  - `NEXT_PUBLIC_ENABLE_MOCK=false` 时走后端
- 部署：
  - `backend/docker-compose.backend.yml` 启动后端+MySQL+Redis
  - 前端通过 `NEXT_PUBLIC_API_BASE_URL` 指向后端
